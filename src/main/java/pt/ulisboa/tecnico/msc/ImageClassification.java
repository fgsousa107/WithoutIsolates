package pt.ulisboa.tecnico.msc;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import pt.ulisboa.tecnico.msc.images.models.inception.InceptionImageClassifier;
import pt.ulisboa.tecnico.msc.images.utils.ResourceUtils;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ImageClassification {

    public static InceptionImageClassifier classifier = null;
    public static MinioClient minioClient = null;

    public static void init_classifier() {
        classifier = new InceptionImageClassifier();
        try {
            // cls.load_model(ResourceUtils.getInputStream("tf_models/tensorflow_inception_graph.pb"));
            minioClient =
                    MinioClient.builder()
                            .endpoint("http://127.0.0.1:9000")
                            .credentials("minioadmin", "minioadmin")
                            .build();

            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("files")
                            .object("tensorflow_inception_graph.pb")
                            .build())) {
                classifier.load_model(is);
            }
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("files")
                            .object("imagenet_comp_graph_label_strings.txt")
                            .build())) {
                classifier.load_labels(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String predict(int index) {
        if (classifier == null){
            init_classifier();
        }
        String[] image_names = new String[] { "tiger", "lion", "airplane", "eagle" };
        String file_name = image_names[index];
        String image_path = file_name + ".jpg";
        BufferedImage img = null;

        try {
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("files")
                            .object(image_path)
                            .build())) {
                img = ResourceUtils.getImage(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String predicted_label = classifier.predict_image(img);

        return predicted_label;
    }

    /*public static void main(String[] args) throws IOException {
        Runtime rt = Runtime.getRuntime();
        int concurrency = 1;
        int number_of_tasks = 100;

        final long free = rt.freeMemory();

        long start = System.currentTimeMillis();
        init_classifier();
        long free_after_model = rt.freeMemory();
        System.out.println("Memory for the model: "+ Double.toString((free-free_after_model)/1000000.));
        long end = System.currentTimeMillis();
        System.out.println("Init time: " + (end - start) / 1000.);

        start = System.currentTimeMillis();
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(concurrency);
        Future[] futures = new Future[number_of_tasks];
        for(int i = 0; i < number_of_tasks; i++){
            futures[i] = tpe.submit(() -> {
                predict(0);
                System.out.println("Memory for the model: "+ Double.toString((free - rt.freeMemory())/1000000.));
                return 0;
            });
        }
        for(int i = 0; i < number_of_tasks; i++){
            try {
                futures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Memory for the model: "+ Double.toString((free - rt.freeMemory())/1000000.));
        System.out.println("EXE time: " + (end - start) / 1000.);

        tpe.shutdown();
    }

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
        boolean slow_start = true;
        synchronized (globals) {
            if (!globals.containsKey("classifier")) {
                init_classifier();
                globals.put("classifier", classifier);
                globals.put("minio", minioClient);
            } else {
                classifier = (InceptionImageClassifier) globals.get("classifier");
                minioClient = (MinioClient) globals.get("minio");
                slow_start = false;
            }
        }

        JsonObject response = predict(args.getAsJsonPrimitive("index").getAsInt());
        response.addProperty("slow_start", slow_start);
        return response;
    }*/
}
