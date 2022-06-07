import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client {

    private static final String[] functions = {"Sleep", "REST", "ClassifyImage", "TransformVideo", "FileHashing", "OneCounter", "Fibonacci"};

    private static final HashMap<String, Integer> nCycles = new HashMap<>();
    private static final HashMap<String, Integer> nThreads = new HashMap<>();
    private static final HashMap<String, Integer> cpus = new HashMap<>();

    static class ClientThread implements Runnable {

        private final String function;
        private final String functionArgument;
        private final Integer nThreads;
        private final Integer nCycles;
        private final Integer cpu;
        private static final List<String> log = new ArrayList<>();

        public ClientThread(String function, String functionArgument, Integer nThreads, Integer nCycles, Integer cpu) {
            this.function = function;
            this.nThreads = nThreads;
            this.nCycles = nCycles;
            this.cpu = cpu;

            if(functionArgument == null) {
                switch(function) {
                    case "Sleep":
                        this.functionArgument = "1000";
                        break;
                    case "REST":
                        this.functionArgument = "Graal";
                        break;
                    case "ClassifyImage":
                    case "TransformVideo":
                        this.functionArgument = "";
                        break;
                    case "FileHashing":
                    case "OneCounter":
                        this.functionArgument = "alice29.txt";
                        break;
                    case "Fibonacci":
                        this.functionArgument = "100000";
                        break;
                    default:
                        this.functionArgument = "";
                }
            }
            else {
                this.functionArgument = functionArgument;
            }
        }

        @Override
        public void run() {
            try {
                URL url = new URL("http://localhost:8080/run");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setDoOutput(true);
                String jsonInputString = "{'function':'" + this.function + "','functionArgument':'" + this.functionArgument + "','nThreads':"
                        + this.nThreads + ",'nCycles':" + this.nCycles + ",'cpu':" + this.cpu + "}";

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    //System.out.println(response.toString());
                    log.add(response.toString());
                }

                con.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void processConfigurationFile() {
        File folder = new File(".");
        File[] listOfFiles = folder.listFiles();

        if(listOfFiles != null) {
            for (File f : listOfFiles) {
                if (f.isFile() && f.getName().endsWith(".txt")) {

                    System.out.println("\nProcessing configuration file: " + f.getName());

                    try(BufferedReader br = new BufferedReader(new FileReader(f.getName()))) {
                        long delay;
                        int conc, threads, time, cpu;
                        String line = br.readLine(); if(line == null) return;

                        line = br.readLine();
                        while (line != null) {
                            String[] args = line.split(" "); if(args.length != 2) return;
                            delay = Integer.parseInt(args[0]);
                            conc = Integer.parseInt(args[1]);

                            System.out.println("\nWaiting " + delay + "ms...");
                            Thread.sleep(delay);
                            System.out.println("\nSending " + conc + " concurrent requests...");
                            for(int i = 0; i < conc; i++) {
                                line = br.readLine(); if(line == null) return;
                                args = line.split(" ");
                                Runnable ct;
                                if(args.length == 4) {
                                    threads = Integer.parseInt(args[1]);
                                    time = Integer.parseInt(args[2]);
                                    cpu = Integer.parseInt(args[3]);
                                    ct = new ClientThread(args[0], null, threads, time, cpu);
                                }
                                else if(args.length == 5) {
                                    threads = Integer.parseInt(args[2]);
                                    time = Integer.parseInt(args[3]);
                                    cpu = Integer.parseInt(args[4]);
                                    ct = new ClientThread(args[0], args[1], threads, time, cpu);
                                }
                                else return;

                                Thread t = new Thread(ct);
                                t.start();

                                System.out.println("\tSent request for the " + args[0] + " function (threads: " +
                                        threads + "; time: " + time + "; cpu: " + cpu + ")");
                            }

                            line = br.readLine();
                        }
                    } catch (IOException | NumberFormatException | InterruptedException e) {
                        System.out.println("\nFailed to read configuration file.");
                    }

                    break;
                }
            }
        }
    }

    public static Integer mainMenu(Scanner in) {
        String line;

        System.out.println("\nSelect a command: \n");
        System.out.println("1. Change functions' parameters (cpu quota, nThreads, nCycles).");
        System.out.println("2. Make function request.");
        System.out.println("3. Process configuration file.");
        System.out.println("4. Exit.\n");
        System.out.print("-->  ");

        if(in.hasNextLine()) {
            line = in.nextLine();

            switch(line)
            {
                case "1":
                    return 1;
                case "2":
                    return 2;
                case "3":
                    return 3;
                case "4":
                    return 4;
                default:
                    System.out.println("Invalid command.");
                    return null;
            }
        }
        return null;
    }

    public static String selectFunctionToAdjustParameters(Scanner in) {
        String line;

        System.out.println("\nSelect a function: \n");
        int i = 1;
        for(String s: functions) {
            System.out.println(i + ". " + s + ".");
            i++;
        }
        System.out.println(i + ". Go back to main menu.\n");
        System.out.print("-->  ");

        if(in.hasNextLine()) {
            line = in.nextLine();

            try {
                i = Integer.parseInt(line);

                if(i == functions.length + 1)
                    return "Back";
                else if(i >= 1 && i <= functions.length)
                    return functions[i - 1];
                else {
                    System.out.println("Invalid command.");
                    return null;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid command.");
                return null;
            }
        }
        return null;
    }

    public static Integer setParameter(Scanner in, String parameter, int current_par, int min, int max) {
        String line;
        int c;

        System.out.print("\nCurrent " + parameter + ": " + current_par);
        System.out.print("\nNew " + parameter + ": ");

        if(in.hasNextLine()) {

            line = in.nextLine();
            try {
                c = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return null;
            }

            if (min != -1 && c < min) {
                System.out.println("The input needs to be bigger than " + min + ".");
                return null;
            }
            if (max != -1 && c > max) {
                System.out.println("The input needs to be smaller than " + max + ".");
                return null;
            }
            return c;
        }
        return null;
    }

    public static Integer selectNumberOfConcurrentRequests(Scanner in) {
        String line;
        int c;

        System.out.print("\nSelect the number of concurrent requests: ");

        if(in.hasNextLine()) {

            line = in.nextLine();
            try {
                c = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return null;
            }

            if (c < 1) {
                System.out.println("The input needs to be bigger than 1.");
                return null;
            }
            return c;
        }
        return null;
    }

    public static String selectFunctionToSendRequest(Scanner in) {
        String line;

        System.out.println("\nSelect a function: \n");
        int i = 1;
        for(String s: functions) {
            System.out.println(i + ". " + s + ".");
            i++;
        }
        System.out.println(i + ". All.\n");
        System.out.print("-->  ");

        if(in.hasNextLine()) {
            line = in.nextLine();

            try {
                i = Integer.parseInt(line);

                if(i == functions.length + 1)
                    return "All";
                else if(i >= 1 && i <= functions.length)
                    return functions[i - 1];
                else {
                    System.out.println("Invalid command.");
                    return null;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid command.");
                return null;
            }
        }
        return null;
    }

    public static void launchThreads(int concurrent, String function) {
        if(Objects.equals(function, "All")) {
            for(int i = 0; i < concurrent; i++) {
                for(String s: functions) {
                    Runnable ct = new ClientThread(s, null, nThreads.get(s), nCycles.get(s), cpus.get(s));
                    Thread t = new Thread(ct);
                    t.start();
                }
            }
        }
        else {
            for (int i = 0; i < concurrent; i++) {
                Runnable ct = new ClientThread(function, null, nThreads.get(function),
                        nCycles.get(function), cpus.get(function));
                Thread t = new Thread(ct);
                t.start();
            }
        }
    }

    public static void printLog() {
        for(String s : ClientThread.log) {
            System.out.println(s);
        }
    }

    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        Integer command, c;
        String function;

        // Set default values for nCycles, nThreads, and cpus
        for(String s: functions) {
            nThreads.put(s, 0);
            nCycles.put(s, 10);
            cpus.put(s, 10);
        }

        while(true) {

            if((command = mainMenu(in)) == null) continue;

            if(command.equals(1)) {
                while(true) {
                    if ((function = selectFunctionToAdjustParameters(in)) == null) continue;
                    if(function.equals("Back")) break;
                    if ((c = setParameter(in, "cpu quota [1,100]", cpus.get(function), 1, 100)) == null) continue;
                    cpus.put(function, c);
                    if ((c = setParameter(in, "number of threads [0,inf]", nThreads.get(function), 0, -1)) == null) continue;
                    nThreads.put(function, c);
                    if ((c = setParameter(in, "number of cycles [1,inf]", nCycles.get(function), 1, -1)) == null) continue;
                    nCycles.put(function, c);
                }
            }
            else if(command.equals(2)) {
                if((c = selectNumberOfConcurrentRequests(in)) == null) continue;
                if((function = selectFunctionToSendRequest(in)) == null) continue;
                launchThreads(c, function);
            }
            else if(command.equals(3)) {
                processConfigurationFile();
                System.out.println("\nFinished processing the configuration file.");
            }
            else if(command.equals(4)) {
                break;
            }
        }
        in.close();
    }
}