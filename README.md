# Simple static web server #

A simple web server which serves static pages from the same directory it is run.

## Usage ##

Add the dependency in the `deps.edn` file:

```edn
{:deps {:io.github.stefanobevilacqua/web-server {:git/tag "0.0.1" :git/sha "b4d92f5"}}}
```

Start the server at port 8080:

```bash
clj -M -m web-server.core -p 8080
```

## Command Line Options ##

| Flag                     | Description                                                        |
|--------------------------|--------------------------------------------------------------------|
| `-p` (or `--port`)       | Specifies the web server port (default is random)                  |
| `-d` (or `--static-dir`) | Specifies the directory containing static pages (default is `"."`) |
| `-s` (or `--pool-size`)  | Specifies the size of the thread pool used to handle requests      |


