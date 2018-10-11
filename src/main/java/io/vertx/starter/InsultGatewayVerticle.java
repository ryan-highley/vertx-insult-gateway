package io.vertx.starter;

import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_SPRINGBOOT_NOUN;
import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_SPRINGBOOT_NOUN_PORT;
import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_VERTX_ADJ;
import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_VERTX_ADJ_PORT;
import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_WILDFLYSWARM_ADJ;
import static io.vertx.starter.ApplicationProperties.GATEWAY_HOST_WILDFLYSWARM_ADJ_PORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

public class InsultGatewayVerticle extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(InsultGatewayVerticle.class);

  private WebClient clientSpringboot;
  private WebClient clientSwarm;
  private WebClient clientVertx;
  private ConfigRetriever conf;

  @Override
  public void start(Future<Void> startFuture) {

    conf = ConfigRetriever.create(vertx);
    Router router = Router.router(vertx);



    clientSpringboot = WebClient.create(vertx,
      new WebClientOptions()
        .setDefaultHost(config().getString(GATEWAY_HOST_SPRINGBOOT_NOUN,
          "springboot-noun-service-devenv-user27.apps.4c8f.rhte.opentlc.com"))
        .setDefaultPort(config().getInteger(GATEWAY_HOST_SPRINGBOOT_NOUN_PORT, 80)));

    clientSwarm = WebClient.create(vertx,
      new WebClientOptions()
        .setDefaultHost(config().getString(GATEWAY_HOST_WILDFLYSWARM_ADJ,
          "wildflyswarm-adj-devenv-user27.apps.4c8f.rhte.opentlc.com"))
        .setDefaultPort(config().getInteger(GATEWAY_HOST_WILDFLYSWARM_ADJ_PORT, 80)));



    clientVertx = WebClient.create(vertx,
      new WebClientOptions()
        .setDefaultHost(config().getString(GATEWAY_HOST_VERTX_ADJ,
          "vertx-adjective-service-devenv-user27.apps.4c8f.rhte.opentlc.com"))
        .setDefaultPort(config().getInteger(GATEWAY_HOST_VERTX_ADJ_PORT, 80)));



    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    router.get("/api/insult").handler(this::insultHandler);
    router.get("/*").handler(StaticHandler.create());


    startFuture.complete();


  }

  Future<JsonObject> getNoun() {
    Future<JsonObject> fut = Future.future();
    clientSpringboot.get("/api/noun").timeout(3000).rxSend()

      .map(HttpResponse::bodyAsJsonObject).doOnError(fut::fail).subscribe(fut::complete);
    return fut;
  }


  Future<JsonObject> getAdjective() {
    Future<JsonObject> fut = Future.future();
    clientSwarm.get("/api/adjective").timeout(3000).rxSend()

      .map(HttpResponse::bodyAsJsonObject).doOnError(fut::fail).subscribe(fut::complete);
    return fut;
  }

  Future<JsonObject> getAdjective2() {
    Future<JsonObject> fut = Future.future();
    clientVertx.get("/api/adjective").timeout(3000).rxSend()

      .map(HttpResponse::bodyAsJsonObject).doOnError(fut::fail).subscribe(fut::complete);
    return fut;
  }

  private AsyncResult<JsonObject> buildInsult(CompositeFuture cf) {
    JsonObject insult = new JsonObject();
    JsonArray adjectives = new JsonArray();
    // Because there is no garanteed order of the returned futures, we need to parse the results

    for (int i = 0; i <= cf.size() - 1; i++) {
      JsonObject item = cf.resultAt(i);
      if (item.containsKey("adjective")) {
        adjectives.add(item.getString("adjective"));
      } else {
        insult.put("noun", item.getString("noun"));
      }

    }
    insult.put("adjectives", adjectives);


    return Future.succeededFuture(insult);
  }

  private void insultHandler(RoutingContext rc) {

    CompositeFuture.all(getNoun(), getAdjective(), getAdjective2()).setHandler(ar -> {

      if (ar.succeeded()) {
        AsyncResult<JsonObject> result = buildInsult(ar.result());
        rc.response().putHeader("content-type", "application/json")
          .end(result.result().encodePrettily());
      } else {
        System.out.println("error");

        rc.response().putHeader("content-type", "application/json")
          .end(new JsonObject("Error").encodePrettily());
      }



    });
  }

}
