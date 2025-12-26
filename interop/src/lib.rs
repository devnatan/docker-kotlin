use hickory_resolver::config::{NameServerConfig, ResolverConfig, ResolverOpts};
use hickory_resolver::TokioAsyncResolver;
use reqwest::Client;
use std::ffi::{CStr, CString};
use std::net::{IpAddr, SocketAddr};
use std::os::raw::c_char;
use std::time::Duration;

#[cfg(unix)]
use std::os::unix::net::UnixStream;
#[cfg(unix)]
use std::path::Path;

#[repr(C)]
pub struct DnsResult {
    pub addresses: *mut *mut c_char,
    pub count: usize,
    pub error: *mut c_char,
}

#[repr(C)]
pub struct HttpResponse {
    pub body: *mut c_char,
    pub status_code: u16,
    pub error: *mut c_char,
}

#[repr(C)]
pub struct HttpRequest {
    pub url: *const c_char,
    pub method: *const c_char,
    pub headers: *mut *mut c_char,
    pub headers_count: usize,
    pub body: *const c_char,
    pub body_len: usize,
    pub timeout_ms: u64,
}

#[no_mangle]
pub extern "C" fn dns_resolve(
    hostname: *const c_char,
    dns_servers: *const *const c_char,
    dns_servers_count: usize,
) -> *mut DnsResult {
    let hostname = unsafe {
        match CStr::from_ptr(hostname).to_str() {
            Ok(s) => s,
            Err(_) => return create_dns_error("Invalid hostname encoding"),
        }
    };

    let dns_server_ips: Vec<IpAddr> = if dns_servers_count > 0 {
        (0..dns_servers_count)
            .filter_map(|i| unsafe {
                let server_ptr = *dns_servers.add(i);
                CStr::from_ptr(server_ptr)
                    .to_str()
                    .ok()
                    .and_then(|s| s.parse::<IpAddr>().ok())
            })
            .collect()
    } else {
        vec![]
    };

    let rt = match tokio::runtime::Runtime::new() {
        Ok(rt) => rt,
        Err(_) => return create_dns_error("Failed to create runtime"),
    };

    rt.block_on(async {
        let resolver = if dns_server_ips.is_empty() {
            match TokioAsyncResolver::tokio_from_system_conf() {
                Ok(r) => r,
                Err(e) => return create_dns_error(&format!("Failed to create system resolver: {}", e)),
            }
        } else {
            let mut config = ResolverConfig::new();

            for ip in dns_server_ips {
                let socket_addr = SocketAddr::new(ip, 53);
                let name_server = NameServerConfig {
                    socket_addr,
                    protocol: hickory_resolver::config::Protocol::Udp,
                    tls_dns_name: None,
                    trust_negative_responses: true,
                    bind_addr: None,
                };
                config.add_name_server(name_server);
            }

            let opts = ResolverOpts::default();

            TokioAsyncResolver::tokio(config, opts)
        };

        match resolver.lookup_ip(hostname).await {
            Ok(lookup) => {
                let addresses: Vec<String> = lookup
                    .iter()
                    .map(|ip| ip.to_string())
                    .collect();

                if addresses.is_empty() {
                    return create_dns_error("No addresses found");
                }

                let c_addresses: Vec<*mut c_char> = addresses
                    .into_iter()
                    .map(|addr| {
                        CString::new(addr).unwrap().into_raw()
                    })
                    .collect();

                let count = c_addresses.len();
                let addresses_ptr = Box::into_raw(c_addresses.into_boxed_slice()) as *mut *mut c_char;

                Box::into_raw(Box::new(DnsResult {
                    addresses: addresses_ptr,
                    count,
                    error: std::ptr::null_mut(),
                }))
            }
            Err(e) => create_dns_error(&format!("DNS lookup failed: {}", e)),
        }
    })
}

fn create_dns_error(message: &str) -> *mut DnsResult {
    Box::into_raw(Box::new(DnsResult {
        addresses: std::ptr::null_mut(),
        count: 0,
        error: CString::new(message).unwrap().into_raw(),
    }))
}

#[no_mangle]
pub extern "C" fn dns_result_free(result: *mut DnsResult) {
    if result.is_null() {
        return;
    }

    unsafe {
        let result = Box::from_raw(result);

        if !result.error.is_null() {
            let _ = CString::from_raw(result.error);
        }

        if !result.addresses.is_null() {
            let addresses = Vec::from_raw_parts(
                result.addresses,
                result.count,
                result.count,
            );
            for addr in addresses {
                if !addr.is_null() {
                    let _ = CString::from_raw(addr);
                }
            }
        }
    }
}

#[no_mangle]
pub extern "C" fn http_request_execute(request: *const HttpRequest) -> *mut HttpResponse {
    if request.is_null() {
        return create_http_error("Null request", 0);
    }

    let request = unsafe { &*request };

    let url = unsafe {
        match CStr::from_ptr(request.url).to_str() {
            Ok(s) => s,
            Err(_) => return create_http_error("Invalid URL encoding", 0),
        }
    };

    let method = unsafe {
        match CStr::from_ptr(request.method).to_str() {
            Ok(s) => s,
            Err(_) => return create_http_error("Invalid method encoding", 0),
        }
    };

    let rt = match tokio::runtime::Runtime::new() {
        Ok(rt) => rt,
        Err(_) => return create_http_error("Failed to create runtime", 0),
    };

    rt.block_on(async {
        let client = match Client::builder()
            .timeout(Duration::from_millis(request.timeout_ms))
            .build()
        {
            Ok(c) => c,
            Err(e) => return create_http_error(&format!("Failed to create client: {}", e), 0),
        };

        let mut req_builder = match method {
            "GET" => client.get(url),
            "POST" => client.post(url),
            "PUT" => client.put(url),
            "DELETE" => client.delete(url),
            "PATCH" => client.patch(url),
            "HEAD" => client.head(url),
            _ => return create_http_error("Unsupported HTTP method", 0),
        };

        for i in 0..request.headers_count {
            unsafe {
                let header_ptr = *request.headers.add(i);
                if let Ok(header_str) = CStr::from_ptr(header_ptr).to_str() {
                    if let Some((key, value)) = header_str.split_once(':') {
                        req_builder = req_builder.header(key.trim(), value.trim());
                    }
                }
            }
        }

        if !request.body.is_null() && request.body_len > 0 {
            let body_slice = unsafe {
                std::slice::from_raw_parts(request.body as *const u8, request.body_len)
            };
            req_builder = req_builder.body(body_slice.to_vec());
        }

        match req_builder.send().await {
            Ok(response) => {
                let status = response.status().as_u16();
                match response.text().await {
                    Ok(body) => {
                        let body_cstring = match CString::new(body) {
                            Ok(s) => s,
                            Err(_) => return create_http_error("Response contains null bytes", status),
                        };

                        Box::into_raw(Box::new(HttpResponse {
                            body: body_cstring.into_raw(),
                            status_code: status,
                            error: std::ptr::null_mut(),
                        }))
                    }
                    Err(e) => create_http_error(&format!("Failed to read response: {}", e), status),
                }
            }
            Err(e) => create_http_error(&format!("Request failed: {}", e), 0),
        }
    })
}

fn create_http_error(message: &str, status: u16) -> *mut HttpResponse {
    Box::into_raw(Box::new(HttpResponse {
        body: std::ptr::null_mut(),
        status_code: status,
        error: CString::new(message).unwrap().into_raw(),
    }))
}

#[no_mangle]
pub extern "C" fn http_response_free(response: *mut HttpResponse) {
    if response.is_null() {
        return;
    }

    unsafe {
        let response = Box::from_raw(response);

        if !response.body.is_null() {
            let _ = CString::from_raw(response.body);
        }

        if !response.error.is_null() {
            let _ = CString::from_raw(response.error);
        }
    }
}

#[cfg(unix)]
#[no_mangle]
pub extern "C" fn unix_socket_connect(path: *const c_char) -> i32 {
    let path_str = unsafe {
        match CStr::from_ptr(path).to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        }
    };

    match UnixStream::connect(Path::new(path_str)) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[cfg(not(unix))]
#[no_mangle]
pub extern "C" fn unix_socket_connect(_path: *const c_char) -> i32 {
    -2  // Not supported on this platform
}

#[no_mangle]
pub extern "C" fn free_cstring(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}

#[no_mangle]
pub extern "C" fn dns_resolve_simple(
    hostname: *const c_char,
) -> *mut DnsResult {
    let hostname = unsafe {
        match CStr::from_ptr(hostname).to_str() {
            Ok(s) => s,
            Err(_) => return create_dns_error("Invalid hostname encoding"),
        }
    };

    let rt = match tokio::runtime::Runtime::new() {
        Ok(rt) => rt,
        Err(_) => return create_dns_error("Failed to create runtime"),
    };

    rt.block_on(async {
        let resolver = match TokioAsyncResolver::tokio_from_system_conf() {
            Ok(r) => r,
            Err(e) => return create_dns_error(&format!("Failed to create resolver: {}", e)),
        };

        match resolver.lookup_ip(hostname).await {
            Ok(lookup) => {
                let addresses: Vec<String> = lookup
                    .iter()
                    .map(|ip| ip.to_string())
                    .collect();

                if addresses.is_empty() {
                    return create_dns_error("No addresses found");
                }

                let c_addresses: Vec<*mut c_char> = addresses
                    .into_iter()
                    .map(|addr| CString::new(addr).unwrap().into_raw())
                    .collect();

                let count = c_addresses.len();
                let addresses_ptr = Box::into_raw(c_addresses.into_boxed_slice()) as *mut *mut c_char;

                Box::into_raw(Box::new(DnsResult {
                    addresses: addresses_ptr,
                    count,
                    error: std::ptr::null_mut(),
                }))
            }
            Err(e) => create_dns_error(&format!("DNS lookup failed: {}", e)),
        }
    })
}