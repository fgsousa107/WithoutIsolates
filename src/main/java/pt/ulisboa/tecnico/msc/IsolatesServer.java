package pt.ulisboa.tecnico.msc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class IsolatesServer {
    private final HttpServer server;

    public IsolatesServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), -1); //-1 means system default value
        this.server.createContext("/run", new RunHandler());
        this.server.setExecutor(Executors.newCachedThreadPool(new MyThreadFactory()));
    }

    public void start() {
        server.start();
    }

    private static void writeResponse(HttpExchange t, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void writeError(HttpExchange t, String errorMessage) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("error", errorMessage);
        writeResponse(t, 502, message.toString());
    }

    private static class MyThreadFactory implements ThreadFactory {
        private int counter = 0;
        public Thread newThread(Runnable r) {
            return new Thread(r, "NI_T " + counter++);
        }
    }

    private static class RunHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException{

            try{
                //get json object from http request body
                InputStream is = t.getRequestBody();
                JsonObject body = JsonParser.parseReader(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
                        .getAsJsonObject();
                String function = body.getAsJsonPrimitive("function").getAsString();
                String functionArgument = body.getAsJsonPrimitive("functionArgument").getAsString();
                int nThreads = body.getAsJsonPrimitive("nThreads").getAsInt();
                int nCycles = body.getAsJsonPrimitive("nCycles").getAsInt();
                int cpu = body.getAsJsonPrimitive("cpu").getAsInt();

                String output = CustomerFunctions.launch(function, functionArgument, nThreads, nCycles);
                System.out.println(output);

                if (output == null) {
                    throw new NullPointerException("The action returned null");
                }
                //System.out.println(output);
                IsolatesServer.writeResponse(t,200, output);

            } catch (Exception e) {
                e.printStackTrace(System.err);
                IsolatesServer.writeError(t,"An error has occurred (see logs for details): "+e);
            }
        }
    }

    public static void main(String[] args){
        IsolatesServer s;

        System.out.println(System.getProperty("java.library.path"));

        try{
            s = new IsolatesServer(8080);
            s.start();
            System.out.println("Service Ready");
        }catch(IOException e){
            e.printStackTrace();
            System.err.println("Proxy server can not be started: "+e);
            System.exit(-1);
        }
    }
}
