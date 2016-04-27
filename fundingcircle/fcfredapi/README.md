# fcfredapi

Sample code for fetching data from the FRED economic data api, and saving to a local sql database.

## Installation

You'll need to:

1. Have a sql server running on localhost. The code is set up for mysql. If you have, say, postgres, you'll want to add the appropriate driver into the project dependencies, and change the configuration map in the "fcfredapi.sql" namespace.

2. Grant privileges to the "clojure" user OR change the config code in the "fcfredapi.sql" to a user with privileges.

3. Set the appropriate password.

   SQL password:
        Either,
        1. Put in a text file called "sql-password.key" in the  project "resources" directory.
        2. Set an env variable called FRED_SQL_PASS.

4. An api key for the FRED API. See "https://research.stlouisfed.org/useraccount/register/step1".

   To make sure the code can find the api key do one of the following:
      1. Put the key in a text file called "fredapikey.key" in the project "resources" directory.
      2. Set an env variable called FRED_API_KEY.

5. Use the sql code in "src/sql/create_db_and_tables.sql" to create the database and two tables.


The following assumes you already have Leiningen and Java installed.

## Usage

   Run this only with empty database tables, otherwise you'll get a lot of 'duplicate record' errors.

   cd into the "fcfredapi" directory
   
   To run in the REPL:
      type `lein repl`
      at the REPL prompt, type `(fred/retrieve-all-series)`

   Or, to run from the command line, first make an uberjar, `lein uberjar`, cd into "target/uberjar" and then:

    `java -jar fcfredapi-0.1.0-standalone.jar fred`

## Other

For the answer to the question:
    "What was the average rate of unemployment for each year starting with 1980 and going up to 2015?"

see `fcfredapi/UNRATE_answer.txt`


## License

Copyright © 2016 M. Fellows

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
