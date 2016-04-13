package co.bk.springbootmongo.cursor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


@RestController
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    public static final String CACHED_FILE = "/dmp/cachedFile";

    @Autowired
    MockMongoClient mockMongoClient;

    /*
     * USE CASE: assumed documents processed are uniform in terms of fields.
     *
     * curl -X GET -H "Content-Type: text/csv" http://localhost:8080/csv
     */
    @RequestMapping(value="/csv", method=RequestMethod.GET)
    public void getCSV(HttpServletResponse response){

        /*
         * A real MongoClient provide access to data via an iterable DBCursor.
         *
         * The mock client simulates a cursor by returning an iterable dataset.
         *
         * MongoClients can safely provide access to large datasets with a cursor as data is lazily loaded.
         *
         * Details:
         * http://api.mongodb.org/java/current/com/mongodb/DBCursor.html
         */
        List<Player> data = mockMongoClient.getPlayers();

        OutputStreamWriter outputWriter = null;

        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition","attachment; filename=myData.csv");
        response.setStatus(HttpStatus.OK.value());

        try {
            OutputStream os= response.getOutputStream();
            OutputStream bos = new BufferedOutputStream(os);
            outputWriter = new OutputStreamWriter(bos);

            outputWriter.write(Player.getCsvHeader());
            outputWriter.write('\n');

            Iterator cursor = data.iterator();
            Player player = null;
            while(cursor.hasNext()) {
                player = (Player) cursor.next();
                outputWriter.write(player.toCsvString());
                outputWriter.write('\n');
            }
            outputWriter.flush();
            outputWriter.close();

        } catch (IOException ioe) {

            logger.info("ExportController exception: " + ioe.getMessage());

        } finally {
            //Non-mocked implementation must close DBCursor.
            //cursor.close();
        }

    }

    /*
     * USE CASE: assumed documents are NOT uniform in terms of fields.
     *
     * Data is processed so that:
     * 1. Simulates getting a JSON file as an inputstream from MongoDB "getJsonFileFromMongo()"
     * 2. Unique CSV headers identified from stream and read data is cached on disk to avoid "second" call to mongo.
     * 3. Cached data re-read and CSV data populated to file for download.
     *
     * curl -X GET -H "Content-Type: text/csv" http://localhost:8080/csvfromfile
     */
    @RequestMapping(value="/csvfromfile", method=RequestMethod.GET)
    public void getCsvStreamFromJSONFile(HttpServletResponse response){

        InputStream is = null;
        TeeInputStream tis = null;

        OutputStreamWriter outputWriter = null;
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition","attachment; filename=myData.csv");
        response.setStatus(HttpStatus.OK.value());

        try {

            /*
             * Cache file on disk after retrieving from mocked mongoDb.
             * TeeInputStream BOTH reads result from stream and writes it to a file via FileOutputStream.
             */
            is = mockMongoClient.getJsonFileFromMongo();
            FileOutputStream fos = new FileOutputStream(new File(CACHED_FILE));
            tis = new TeeInputStream(is, fos);

            Set<String> headers = identifyHeaders(tis);

            OutputStream os= response.getOutputStream();
            OutputStream bos = new BufferedOutputStream(os);
            outputWriter = new OutputStreamWriter(bos);

            // Write CSV header row
            writeCsvHeader(outputWriter, headers);

            // Write CSV data
            writeCsvData(outputWriter, headers);

            outputWriter.flush();
            outputWriter.close();

        } catch (IOException e) {

            logger.info("ExportController.getCsvStreamFromJSONFile() exception: " + e.getMessage());

        } finally {
            //Non-mocked implementation must close DBCursor.
            //cursor.close();

            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(tis);
        }

    }


    private Set<String> identifyHeaders(InputStream is) {

        Set<String> headers = new LinkedHashSet<String>();

        JsonObjectIterator joi = new JsonObjectIterator(is);

        Map<String, Object> obj = null;
        while (joi.hasNext()) {
            obj = joi.next();
            for (String s : obj.keySet()) {
                headers.add(s);
            }
        }
        return headers;
    }

    private void writeCsvHeader(OutputStreamWriter outputWriter, Set<String> headers) {

        try {

            StringJoiner joiner = new StringJoiner(",");
            headers.forEach(joiner::add);
            String joined = joiner.toString();
            outputWriter.write(joined);
            outputWriter.write('\n');
        } catch (Exception e) {
            logger.info("ExportController.writeCsvHeader() exception: " + e);
        }
    }

    private void writeCsvData(OutputStreamWriter outputWriter, Set<String> headers) {

        try {

            // Read cached data and write CSV data
            FileInputStream fis = new FileInputStream(new File(CACHED_FILE));
            JsonObjectIterator joi = new JsonObjectIterator(fis);

            List<String> headersList = new ArrayList<String>(headers);

            Map<String, Object> obj = null;
            while (joi.hasNext()) {
                obj = joi.next();

                // Identify position of data (relative to CSV header fields)
                Map<Integer, String> csvPositionAndData = new HashMap<>();
                for (String s : obj.keySet()) {
                    int pos = headersList.indexOf(s);
                    if (pos > -1) {
                        csvPositionAndData.put(new Integer(pos), (String) obj.get(s));
                    }
                }

                // Write data at correct CSV position (relative to CSV header fields)
                StringJoiner sj = new StringJoiner(",");
                for (int i=0; i < headers.size(); i++) {

                    if (csvPositionAndData.containsKey(new Integer(i))) {
                        sj.add(csvPositionAndData.get(new Integer(i)));
                    } else {
                        sj.add("");
                    }
                }
                String csvRow = sj.toString();
                outputWriter.write(csvRow);
                outputWriter.write('\n');
            }

        } catch (Exception e) {
            logger.info("ExportController.writeCsvData() exception: " + e);
        }
    }
}
