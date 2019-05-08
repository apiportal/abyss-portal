# Abyss Portal

This is the repository for Abyss Portal 




## Build and Run
In order to create a fat jar package, install jdk >= 8 and Maven; afterwards, run this command:

```bash
mvn clean package
```

## Publish sites with Maven scm publish plugin

Execute following commands to publish sites:
```
mvn clean install site site:stage scm-publish:publish-scm
```