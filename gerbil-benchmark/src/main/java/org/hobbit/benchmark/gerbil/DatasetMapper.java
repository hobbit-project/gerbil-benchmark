package org.hobbit.benchmark.gerbil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DatasetMapper {

  
    public static String getName(String localUriLabel){
	Properties datasetMapping = new Properties();
	try {
	    datasetMapping.load(ClassLoader.getSystemResource("datasetMapper.properties").openStream());
	} catch (IOException e) {
	    e.printStackTrace();
	    return "";
	}
	return datasetMapping.getProperty(localUriLabel);
    }
    
}
