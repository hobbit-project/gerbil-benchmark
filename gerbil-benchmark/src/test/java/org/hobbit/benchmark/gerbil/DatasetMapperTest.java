package org.hobbit.benchmark.gerbil;

import static org.junit.Assert.*;

import org.junit.Test;

public class DatasetMapperTest {

     @Test
     public void test(){
	 assertEquals("ACE2004", DatasetMapper.getName("ACE2004"));
	 assertEquals("Senseval 2", DatasetMapper.getName("Senseval2"));
     }
}
