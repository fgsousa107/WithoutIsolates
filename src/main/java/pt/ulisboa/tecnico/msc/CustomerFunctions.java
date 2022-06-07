package pt.ulisboa.tecnico.msc;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class CustomerFunctions {

    public static String launch(String function, String functionArgument, int nThreads, int nCycles){
        String response;
        long start = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>();

        // RUN ADDITIONAL THREADS FOR CPU UTILIZATION ANALYSIS
        for(int i = 0; i < nThreads; i++) {
            Thread thread = new Thread(() -> {
                //System.out.println("IS - " + Thread.currentThread().getId() + ", " + CurrentIsolate.getIsolate().rawValue());
                executeFunction(function, functionArgument, nCycles);
            });
            threads.add(thread);
            thread.start();
        }

        // RUN REAL INTENDED COMPUTATION
        response = executeFunction(function, functionArgument, nCycles);

        // JOIN ADDITIONAL THREADS
        for(Thread t: threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        System.out.println(function + " time - " + (end - start) + " ms");
        return response;
    }

    private static String executeFunction(String function, String functionArgument, int nCycles) {
        String response = null;
        long t1 = System.nanoTime();
        long t2 = System.nanoTime();
        double timeElapsed = (t2 - t1) / Math.pow(10, 9);

        while(timeElapsed <= nCycles) {
            switch (function) {
                case "Sleep":
                    long time = Long.parseLong(functionArgument);
                    response = sleep(time);
                    break;
                case "REST":
                    response = mongoDBQuery(functionArgument);
                    break;
                case "ClassifyImage":
                    response = classifyImage();
                    break;
                case "TransformVideo":
                    response = transformVideo();
                    break;
                case "FileHashing":
                    response = digestSHA256(functionArgument);
                    break;
                case "OneCounter":
                    response = countOneBits(functionArgument);
                    break;
                case "Fibonacci":
                    int fibTerm = Integer.parseInt(functionArgument);
                    response = fibonacci(fibTerm);
                    break;
            }
            t2 = System.nanoTime();
            timeElapsed = (t2 - t1) / Math.pow(10, 9);
        }

        return response;
    }

    // CUSTOMER FUNCTIONS

    private static String sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "Slept for " + time + "ms.";
    }

    private static String mongoDBQuery(String name) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
        MongoDatabase database = mongoClient.getDatabase("graal");
        MongoCollection<Document> collection = database.getCollection("names");
        FindIterable<Document> findIterable = collection.find(com.mongodb.client.model.Filters.eq("name", name));
        for (Document d : findIterable) {
            mongoClient.close();
            return "True";
        }

        mongoClient.close();
        return "False";
    }

    private static String classifyImage() {
        ImageClassification.init_classifier();
        return ImageClassification.predict(0);
    }

    private static String transformVideo() {
        FFMPEG.init_classifier();;
        FFMPEG.transform(0);
        return "Done";
    }

    private static String digestSHA256(String file) {

        try {
            URL url = new URL("http://localhost:8000/" + file);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String line;
            while((line = reader.readLine()) != null) {
                md.update(line.getBytes(StandardCharsets.UTF_8));
            }
            reader.close();
            con.disconnect();

            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < digest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
            }
            return hexString.toString();

        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String countOneBits(String file) {

        try {
            URL url = new URL("http://localhost:8000/" + file);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();

            int b, count = 0;
            while((b = is.read()) != -1) {
                for(int i = 0; i < 8; i++) {
                    int n = b & 1;
                    b = b >>> 1;

                    if(n == 1) count++;
                }
            }
            is.close();
            con.disconnect();

            return "There are " + count + " 1 bits in the file " + file;

        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String fibonacci(int term){
        if(term == 1 || term == 2){
            return "1";
        }
        int f1 = 1, f2 = 1, t = 2;
        while(t != term){
            int new_term = f1 + f2;
            f1 = f2;
            f2 = new_term;
            t++;
        }
        return String.valueOf(f2);
    }
}