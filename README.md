# FCP API

## Usage:
```
GET http://localhost:8080/fcp?iri=https://owl.vse.cz/ontologies/117810.owl&selectedClasses=http://cmt#Administrator,http://cmt#Author
```
- The `iri` param is required and should be the IRI of the ontology, which should be dereferencable, otherwise use a direct download link of the ontology source code
- The `selectedClasses` param is used to filter focus classes

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

