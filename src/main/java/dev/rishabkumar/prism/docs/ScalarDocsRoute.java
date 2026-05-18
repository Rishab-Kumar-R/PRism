package dev.rishabkumar.prism.docs;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ScalarDocsRoute {

    @Route(path = "/q/docs", methods = Route.HttpMethod.GET)
    void docs(RoutingContext rc) {
        rc.response()
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end("""
                        <!doctype html>
                        <html lang="en">
                          <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>PRism API Docs</title>
                          </head>
                          <body>
                            <div id="app"></div>
                            <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                            <script>
                              Scalar.createApiReference('#app', {
                                url: '/q/openapi',
                                configuration: {
                                  title: 'PRism API',
                                  theme: 'default',
                                  layout: 'modern',
                                  defaultHttpClient: {
                                    targetKey: 'shell',
                                    clientKey: 'curl'
                                  }
                                }
                              })
                            </script>
                          </body>
                        </html>
                        """);
    }
}
