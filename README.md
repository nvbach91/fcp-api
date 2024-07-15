# FCP API
## Focused Categorization Power of Ontologies

Focused Categorization Power (FCP) is a specific quality measure of ontologies based on class subcategorization in these ontologies. The subcategorization can be expressed via named subclasses and also compound concept expressions.

FCP API (back-end) is the API used to extract concepts and 4 types of subcategorization from a specified ontology. This information is then consumed by the front-end ([OReCaP](https://github.com/nvbach91/orecap)), where users are able to use keywords to search for ontologies and from a user-selected Focus class calculate the FCP value in each relevant ontology. OReCaP also attempts to show users the process of FCP calculation, hence, users are also able to specifically view each subcategorizations in their respective categories suggested by the FCP calculation method.

This repository contains the code of the back-end part of this FCP project.

## Link to web application (front-end)
https://fcp.vse.cz/orecap

## Front-end (UI) project repository
https://github.com/nvbach91/orecap


## Usage:
```
GET http://localhost:8080/fcp
      ?iri            =https://owl.vse.cz/ontologies/117810.owl
      &selectedClasses=http://cmt#Administrator,http://cmt#Author
```
- The `iri` param is required and should be the IRI of the ontology, which should be dereferencable, otherwise use a direct download link of the ontology source code
- The `selectedClasses` param is used to define the desired focus classes

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
- Deploy the `/build/libs/fcpapi.war` file to a Tomcat container
    - make sure Tomcat has permission to write file to CATALINA_BASE

