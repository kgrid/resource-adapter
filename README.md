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

##Guidance for Knowledge Object Developers
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
      - "src/hello.txt"
      - "src/goodbye.txt"
    engine: "resource"
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

##Examples
An example KO can be found in our [example collection](https://github.com/kgrid-objects/example-collection/releases/latest) here:
[resource/simple/1.0](https://github.com/kgrid-objects/example-collection/releases/download/4.1.0/resource-simple-v1.0.zip)

##Importnat Notes
- If the artifact is not specified under the `artifact` node in the deployment spec, it will not be available to `GET`.
- If the artifact is specified in the deployment spec, but not actually in the KO, the endpoint will return 404.