# Simple static web server #

A simple web server which serves static pages from the same directory it is run.

## Usage ##

Add the dependency in the `deps.edn` file:

```edn
{:deps {:io.github.stefanobevilacqua/web-server {:git/tag "0.0.1" :git/sha "6747f98"}}}
```

Start the server at port 8080:

```bash
clj -M -m web-server.core -p 8080
```

## Command Line Options ##

| Flag                     | Description                                                                                         |
|--------------------------|-----------------------------------------------------------------------------------------------------|
| `-p` (or `--port`)       | Specifies the web server port (default is random)                                                   |
| `-d` (or `--static-dir`) | Specifies the directory containing static pages (default is `"."`)                                  |
| `-s` (or `--pool-size`)  | Specifies the size of the thread pool used to handle requests (default is twice the n of cpu cores) |


