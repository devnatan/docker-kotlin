# docker-kotlin supported Docker API endpoints

Supports 49 of 107 endpoints

### Containers (16/26)
* [x] List containers - GET **/containers/json**
* [x] Create a container - POST **/containers/create**
* [x] Inspect a container - GET **/containers/:id/json**
* [ ] List processes running inside a container - **GET /containers/:id/top**
* [x] Get container logs - **GET /containers/:id/logs**
* [ ] Get changes on container's filesystem - **GET /containers/:id/changes**
* [ ] Export a container - **GET /containers/:id/export**
* [ ] Get container stats based on resource usage - **GET /containers/:id/stats**
* [x] Resize a container TTY - **POST /containers/:id/resize**
* [x] Start a container - **POST /containers/:id/start**
* [x] Stop a container - **POST /containers/:id/stop**
* [x] Restart a container - **POST /containers/:id/restart**
* [x] Kill a container - **POST /containers/:id/kill**
* [ ] Update a container - **POST /containers/:id/update**
* [x] Rename a container - **POST /containers/:id/rename**
* [x] Pause a container - **POST /containers/:id/pause**
* [x] Unpause a container - **POST /containers/:id/unpause**
* [ ] Attach to a container (HTTP) - **POST /containers/:id/attach**
* [ ] Attach to a container (TCP) - **POST /containers/:id/attach**
* [x] Attach to a container (WebSocket) - **POST /containers/:id/attach/ws**
* [x] Wait for a container - **POST /containers/:id/wait**
* [x] Remove a container - **DELETE /containers/:id**
* [x] Get information about files in a container - **HEAD /containers/:id/archive**
* [x] Get an archive of a filesystem resource in a container - **GET /containers/:id/archive**
* [x] Extract an archive of files or folders to a directory in a container - **PUT /containers/:id/archive**
* [x] Delete stopped containers - **PUT /containers/prune**

### Images (4/15)
* [x] List images - GET **/images/json**
* [ ] Build an image - POST **/build**
* [ ] Delete builder cache - POST **/build/prune**
* [ ] Create an image - POST **/images/create**
* [x] Pull an image - POST **/images/create**
* [x] Inspect an image - GET **/images/:name/json**
* [ ] Get the history of an image - GET **/images/:name/history**
* [ ] Push an image to a registry - POST **/images/:name/push**
* [ ] Tag an image - POST **/images/:name/tag**
* [x] Remove an image - DELETE **/images/:name**
* [ ] Search images - GET **/images/search**
* [ ] Delete unused images - POST **/images/prune**
* [ ] Create a new image from a container - POST **/commit**
* [ ] Export and image - GET **/images/:name/get**
* [ ] Export several images - GET **/images/get**
* [ ] Import images - POST **/images/load**

### Network (7/7)
* [x] List networks - GET **/networks**
* [x] Inspect a network - GET **/networks/:id**
* [x] Remove a network - DELETE **/networks/:id**
* [x] Create a network - POST **/networks/create**
* [x] Connect a container to a network - POST **/networks/:id/connect**
* [x] Disconnect a container from a network - POST **/networks/:id/disconnect**
* [x] Delete unused networks - POST **/networks/prune**

### Volumes (5/5)
* [x] List volumes - GET **/volumes**
* [x] Create a volume - POST **/volumes/create**
* [x] Inspect a volume - GET **/volumes/:name**
* [x] Remove a volume - DELETE **/volumes/:name**
* [x] Delete unused volumes - POST **/volumes/prune**

### Exec (4/4)
* [x] Create a nexec instance - POST **/containers/:id/exec**
* [x] Start an exec instance - POST **/exec/:id/start**
* [x] Resize an exec instance - POST **/exec/:id/resize**
* [x] Inspect an exec instance - GET **/exec/:id/json**

### Swarm (0/7)
* [ ] Inspect swarm - GET **/swarm**
* [ ] Initialize a new swarm - POST **/swarm/init**
* [ ] Join an existing swarm - POST **/swarm/join**
* [ ] Leave a swarm - POST **/swarm/leave**
* [ ] Update a swarm - POST **/swarm/update**
* [ ] Get swarm unlock key - GET **/swarm/unlockkey**
* [ ] Unlock a swarm locked manager - POST **/swarm/unlock**

### Nodes (0/4)
* [ ] List nodes - GET **/nodes**
* [ ] Inspect a node - GET **/nodes/:id**
* [ ] Delete a node - DELETE **/nodes/:id**
* [ ] Update a node - POST **/nodes/:id/update**

### Services (0/6)
* [ ] List services - GET **/services**
* [ ] Create a service - POST **/services/create**
* [ ] Inspect a service - GET **/services/:id**
* [ ] Delete a service - DELETE **/services/:id**
* [ ] Update a service - POST **/services/:id/update**
* [ ] Get service logs - GET **/services/:id/logs**

### Tasks (0/3)
* [ ] List tasks - GET **/tasks**
* [ ] Inspect a task - GET **/tasks:id**
* [ ] Get task logs - GET **/tasks/:id/logs**

### Secrets (5/5)
* [x] List secrets - GET **/secrets**
* [x] Create a secret - POST **/secrets/create**
* [x] Inspect a secret - GET **/secrets/:id**
* [x] Delete a secret - DELETE **/secrets/:id**
* [x] Update a secret - POST **/secrets/:id/update**

### Configs (0/5)
* [ ] List configs - GET **/configs**
* [ ] Create a config - POST **/configs/create**
* [ ] Inspect a config - GET **/configs/:id**
* [ ] Delete a config - DELETE **/configs/:id**
* [ ] Update a config - POST **/configs/:id/update**

### Plugins (0/11)
* [ ] List plugins - GET **/plugins**
* [ ] Get plugin privileges - GET **/plugins/privileges**
* [ ] Install a plugin - POST **/plugins/pull**
* [ ] Inspect a plugin - GET **/plugins/:name/json**
* [ ] Remove a plugin - DELETE **/plugins/:name**
* [ ] Enable a plugin - POST **/plugins/:name/enable**
* [ ] Disable a plugin - POST **/plugins/:name/disable**
* [ ] Upgrade a plugin - POST **/plugins/:name/upgrade**
* [ ] Create a plugin - POST **/plugins/create**
* [ ] Push a plugin - POST **/plugins/:name/push**
* [ ] Configure a plugin - POST **/plugins/:name/set**

### System (3/7)
* [ ] Check auth configuration - POST **/auth**
* [x] Get system information - GET **/info**
* [x] Get version - GET **/version**
* [x] Ping - GET **/_ping**
* [x] Ping - HEAD **/_ping**
* [x] Monitor events - GET **/events**
* [ ] Get data usage information - GET **/system/df**

### Distribution (0/1)
* [ ] Get image information from the registry - GET **/distribution/:name/json**

### Session (0/1)
* [ ] Initialize interactive session - POST **/session**