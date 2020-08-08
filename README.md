# FCP API

## Usage:
```
GET http://localhost:8080/fcp?iris=https://owl.vse.cz/ontologies/117810.owl,http://oaei.ontologymatching.org/2015/conference/data/edas.owl
```

## Output:
```js
{
  "https://owl.vse.cz/ontologies/117810.owl": {
    "v1": [
      "$v1 | <http://cmt#Author> | [[<http://cmt#AuthorNotReviewer>], [<http://cmt#Co-author>]]", // ...
    ],
    "v2": [
      "$v2 | exists | <http://cmt#acceptPaper>.owl:Thing | rdfs:subClassOf | <http://cmt#Administrator>", // ...
    ],
    "v3": [
    ],
    "v4": [
    ]
  },
  // ...
}
```

## Deployment:
- Run `./gradlew bootWar`
- Deploy the `.war` file to a Tomcat container
    - make sure Tomcat has permission to write file to CATALINA_BASE

