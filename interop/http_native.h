#ifndef HTTP_NATIVE_H
#define HTTP_NATIVE_H

#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

typedef struct DnsResult {
  char **addresses;
  uintptr_t count;
  char *error;
} DnsResult;

typedef struct HttpResponse {
  char *body;
  uint16_t status_code;
  char *error;
} HttpResponse;

typedef struct HttpRequest {
  const char *url;
  const char *method;
  char **headers;
  uintptr_t headers_count;
  const char *body;
  uintptr_t body_len;
  uint64_t timeout_ms;
} HttpRequest;

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus

struct DnsResult *dns_resolve(const char *hostname,
                              const char *const *dns_servers,
                              uintptr_t dns_servers_count);

void dns_result_free(struct DnsResult *result);

struct HttpResponse *http_request_execute(const struct HttpRequest *request);

void http_response_free(struct HttpResponse *response);

int32_t unix_socket_connect(const char *path);

int32_t unix_socket_connect(const char *_path);

void free_cstring(char *ptr);

struct DnsResult *dns_resolve_simple(const char *hostname);

#ifdef __cplusplus
}  // extern "C"
#endif  // __cplusplus

#endif  /* HTTP_NATIVE_H */
