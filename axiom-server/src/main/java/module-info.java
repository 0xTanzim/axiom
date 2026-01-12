module io.axiom.server {
    requires io.axiom.core;
    requires jdk.httpserver;
    requires java.net.http;

    exports io.axiom.server;

    provides io.axiom.core.server.ServerFactory
        with io.axiom.server.JdkServerFactory;
}
