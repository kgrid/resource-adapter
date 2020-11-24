# Resource Adapter
This adapter plugs into the Kgrid Activator and exposes artifacts listed under the GET node in the deployment spec.
See the example collection for a simple hello world object.

Example deployment descriptor:
```yaml
/welcome:
  get:
    artifact:
      - "src/hello.txt"
    engine: "resource"
```

### Clone
To get started you can simply clone this repository using git:
```
git clone https://github.com/kgrid/resource-adapter.git
cd resource-adapter
```
Install the adapters to your local maven repository where they can then be used as dependencies by the activator:
```
mvn clean install
```