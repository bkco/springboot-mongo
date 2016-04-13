package co.bk.springbootmongo.cursor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Class enables JSON objects to be iterated from <code>java.io.InputStream</code>.
 */
public class JsonObjectIterator implements Iterator<Map<String, Object>>, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(JsonObjectIterator.class);

    private final InputStream inputStream;
    private JsonParser jsonParser;
    private boolean isInitialized;

    private Map<String, Object> nextObject;

    public JsonObjectIterator(final InputStream inputStream) {
        this.inputStream = inputStream;
        this.isInitialized = false;
        this.nextObject = null;
    }

    private void init() {
        this.initJsonParser();
        this.initFirstElement();
        this.isInitialized = true;
    }

    private void initJsonParser() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonFactory jsonFactory = objectMapper.getFactory();

        try {
            this.jsonParser = jsonFactory.createParser(inputStream);
        } catch (final IOException e) {
            logger.error("There was a problem setting up the JsonParser: " + e.getMessage(), e);
            throw new RuntimeException("There was a problem setting up the JsonParser: " + e.getMessage(), e);
        }
    }

    private void initFirstElement() {
        try {
            final JsonToken arrayStartToken = this.jsonParser.nextToken();
            if (arrayStartToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("The first element of the Json structure was expected to be a start array token, but it was: " + arrayStartToken);
            }

            this.initNextObject();
        } catch (final Exception e) {
            logger.error("There was a problem initializing the first element of the Json Structure: " + e.getMessage(), e);
            throw new RuntimeException("There was a problem initializing the first element of the Json Structure: " + e.getMessage(), e);
        }

    }

    private void initNextObject() {
        try {
            final JsonToken nextToken = this.jsonParser.nextToken();

            // Check for the end of the array which will mean we're done
            if (nextToken == JsonToken.END_ARRAY) {
                this.nextObject = null;
                return;
            }

            // Make sure the next token is the start of an object
            if (nextToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("The next token of Json structure was expected to be a start object token, but it was: " + nextToken);
            }

            // Get the next product and make sure it's not null. <String, Object> --> K, V in json.
            this.nextObject = this.jsonParser.readValueAs(new TypeReference<Map<String, Object>>() { });
            if (this.nextObject == null) {
                throw new IllegalStateException("The next parsed object of the Json structure was null");
            }
        } catch (final Exception e) {
            logger.error("There was a problem initializing the next Object: " + e.getMessage(), e);
            throw new RuntimeException("There was a problem initializing the next Object: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasNext() {
        if (!this.isInitialized) {
            this.init();
        }
        return this.nextObject != null;
    }

    @Override
    public Map<String, Object> next() {

        if (!this.isInitialized) {
            this.init();
        }

        final Map<String, Object> currentNextObject = this.nextObject;

        this.initNextObject();

        return currentNextObject;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.jsonParser);
        IOUtils.closeQuietly(this.inputStream);
    }

}
