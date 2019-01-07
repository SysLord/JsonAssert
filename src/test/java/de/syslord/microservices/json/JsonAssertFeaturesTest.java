package de.syslord.microservices.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonAssertFeaturesTest {

	private static class RootClass {
		@JsonProperty
		private String theString;
		@JsonProperty
		private int theInt;
		@JsonProperty
		private Object theNull;
		@JsonProperty
		private List<Object> theList;
		@JsonProperty
		private Object[] theArray;
		@JsonProperty
		private SubClassA theSubClassA;
		@JsonProperty
		private SubClassA[] theSubClassAArray;
		@JsonProperty
		private List<SubClassA> theSubClassAList;

		public RootClass() {
			// do not use to initialize, jackson will use it too
		}

		protected void init() {
			theString = "aString";
			theInt = 1337;
			theNull = null;
			theList = new ArrayList<>();
			theList.add("a");
			theList.add(1);
			theList.add(9.5);

			theArray = new Object[] { "a", 1, 9.5 };
			theSubClassA = new SubClassA();
			theSubClassAArray = new SubClassA[] { new SubClassA() };
			theSubClassAList = new ArrayList<>();
		}
	}

	private static class SubClassA {
		@JsonProperty
		String theSubStringA = "subStringA";
	}

	@BeforeClass
	public static void setup() {
		JsonAssert.debugOut = System.out;
	}

	@Test
	public void compareObjectsWithoutErrors() {
		RootClass testclass = new RootClass();
		testclass.init();
		List<String> errors = JsonAssert.compareObjects(testclass);
		assertThat(errors).isEmpty();
	}
}
