# Resource Adapter
This adapter plugs into the Kgrid Activator and exposes artifacts listed under the GET node in the deployment spec.

## Installation

This is an embedded runtime, already pulled in by the activator
as a dependency. If you'd like to include it in your maven project,
do so by adding the following dependency to your pom.xml file:
```
<dependency>
  <groupId>org.kgrid</groupId>
  <artifactId>resource-adapter</artifactId>
</dependency>
```

## Configuration
There are currently no configurable settings for this adapter.

## Start the runtime
As an embedded adapter, this will automatically be enabled when the activator starts.

## Examples
An example KO can be found in our [example collection](https://github.com/kgrid-objects/example-collection/releases/latest) here:
[resource/simple/1.0](https://github.com/kgrid-objects/example-collection/releases/latest/download/resource-simple-v1.0.zip)


## Guidance for Knowledge Object Developers
This adapter uses the following endpoints in the activator:

`
GET <activator url>/resource/<naan>/<name>/<api version>/<endpoint>
`

and

`
GET <activator url>/resource/<naan>/<name>/<api version>/<endpoint>/<filename>
`

where the former endpoint returns a list of available resources
described in the deployment spec of the KO, and
the latter endpoint returns the specified file described in the
deployment specification of the KO.

An example KO with naan of `hello`, a name of `neighbor`, api version of `1.0`, and endpoint `welcome`,
 a Deployment Specification might look like this:

```yaml
/welcome:
  get:
    artifact:
      - src/hello.txt
      - src/goodbye.txt
    engine: resource
```

Notice that if the artifacts you want to expose are contained in a folder,
(in this example the `src` directory), that directory needs to be included in the
artifact name.

So to get `hello.txt`, the endpoint would be:

`GET localhost:8080/resource/hello/neighbor/1.0/welcome/src/hello.txt`

The KO directory structure would look as such:

```
- hello-neighbor-1.0
    metadata.json
    service.yaml
    deployment.yaml
    - src
      - hello.txt
      - goodbye.txt
```

Likewise the Service Specification would look like this

```yaml
openapi: 3.0.2
info:
  version: '1.0'
  title: 'Resource KO Example'
  description: An example of simple resource Knowledge Object
  license:
    name: GNU General Public License v3 (GPL-3)
    url: >-
      https://tldrlegal.com/license/gnu-general-public-license-v3-(gpl-3)#fulltext
  contact:
    name: KGrid Team
    email: kgrid-developers@umich.edu
    url: 'http://kgrid.org'
servers:
  - url: /hello/neighbor
    description: Hello world
tags:
  - name: KO Endpoints
    description: Hello world Endpoints
paths:
  /welcome:
    get:
      parameters:
        - in: query
          name: v
          schema:
            type: string
            default:
              $ref: '#/info/version'
          required: true
          description: the api version of the endpoint
...
```

In the Service Specification the servers.url must match the naan and name of the object (`/js/neighbor`) and the path must match the path in Deployment Specification (`/welcome`).
The service spec conforms to the swagger [OpenAPI spec.](https://swagger.io/specification/)


## Important Notes
- If the artifact is not specified under the `artifact` node in the deployment spec, it will not be available to `GET`.
- If the artifact is specified in the deployment spec, but not actually in the KO, the endpoint will return 404.
- For an artifact to be available through the API it must be specified as an artifact in the Deployment Specification
- You can see a list of available artifacts in an endpoint by going to that endpoint directly. Eg: `GET localhost:8080/resource/hello/neighbor/1.0/welcome` will display the artifacts `hello.txt` and `goodbye.txt` in an array.
- The maximum size of a file is set using the properties `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` in the activator and the default for each is 100 MB. 
