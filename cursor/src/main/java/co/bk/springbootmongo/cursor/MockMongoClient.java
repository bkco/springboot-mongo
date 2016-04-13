package co.bk.springbootmongo.cursor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class stubs a MongoClient returning an iterable dataset.
 */
@Service("mockMongoClient")
public class MockMongoClient {

    private static final Logger logger = LoggerFactory.getLogger(MockMongoClient.class);

    private List<Player> players = new ArrayList();

    @Autowired
    private ResourceLoader resourceLoader;

    /*
     * Springboot running as a jar doesn't support getFile(), use getInputStream().
     */
    @PostConstruct
    public void init() {
        try {
            logger.info("MockMongoClient - starting to load JSON");

            Resource resource = resourceLoader.getResource("classpath:testSimple.json");

            ObjectMapper mapper = new ObjectMapper();
            players = mapper.readValue(resource.getInputStream(), new TypeReference<List<Player>>(){});

            logger.info("MockMongoClient - players loaded successfully");

        } catch (IOException | NullPointerException e) {
            logger.error("MockMongoClient - JSON not loaded ", e);
        }
    }

    /**
     * Method simulates an Iterable list similar to DBCursor in mongoDB land.
     * @return list of player objects
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Load a large JSON file stored in mongodb into a stream.
     *
     * MongoDB uses Gridfs to store files greater than 16MB.
     *
     * http://api.mongodb.org/java/current/com/mongodb/gridfs/GridFSDBFile.html
     *
     * @return inputstream
     */
    public InputStream getJsonFileFromMongo() {
        InputStream is = null;

        try {
            is = MockMongoClient.class.getResourceAsStream("/testComplex.json");
        } catch (Exception e) {
            logger.error("MockMongoClient - error getting data stream: ", e);
        }
        return is;
    }

}
