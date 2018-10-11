package io.vertx.starter;

import io.reactivex.Maybe;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {

    initConfigRetriever()
    .doOnError(startFuture::fail)
    .subscribe(ar -> {
      vertx.deployVerticle(InsultGatewayVerticle.class.getName(), new DeploymentOptions().setConfig(ar));

      startFuture.complete();
    });
    
  }
  
  private Maybe<JsonObject> initConfigRetriever() {

    // Load the default configuration from the classpath
    ConfigStoreOptions localConfig = new ConfigStoreOptions()
      .setType("configmap")
      .setFormat("json")
      .setOptional(true)
      .setConfig(new JsonObject().put("path", "conf/insult-config.json"));

    // Add the default and container config options into the ConfigRetriever
    ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions()
      .addStore(localConfig);

    // Create the ConfigRetriever and return the Maybe when complete
    return ConfigRetriever.create(vertx, retrieverOptions).rxGetConfig().toMaybe();
  }

}
