# searchIndex API

The base URL is: [https://search.ezcampus.org/searchIndex/](https://search.ezcampus.org/searchIndex/)

## Schools

### List all schools

GET - /school

#### Example
<p>Request: https://search.ezcampus.org/searchIndex/school</p>
<p>Response: `[{"schoolId":1,"schoolUniqueValue":"Ontario Tech University - Canada","subdomain":"otu","timezone":"America/Toronto"},{"schoolId":2,"schoolUniqueValue":"Durham College - Canada","subdomain":"dc","timezone":"America/Toronto"},{"schoolId":3,"schoolUniqueValue":"University of Victoria - Canada","subdomain":"uv","timezone":"America/Vancouver"}]`</p>

### Get school by id

GET - /school/{id}

#### Example
<p>Request: https://search.ezcampus.org/searchIndex/school/1</p>
<p>Response: `{"schoolId":1,"schoolUniqueValue":"Ontario Tech University - Canada","subdomain":"otu","timezone":"America/Toronto"}`</p>

## List of Classes at a University

