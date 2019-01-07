package de.syslord.microservices.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonAssert {

	public static final ObjectMapper mapper = new ObjectMapper();
	public static PrintStream debugOut = new PrintStream(new ByteArrayOutputStream());

	/**
	 * Uses JsonProperty annotation in fields to compare given instance with
	 * serialized and deserialized instance.
	 *
	 * - Nested Objects with JsonProperty annotated fields will also be compared by
	 * field value.
	 *
	 * - arrays and lists are compared by element.
	 *
	 * - Objects without JsonProperty fields are compared by equal.
	 *
	 */
	public static <T> void assertEqualUsingJsonProperty(T object) throws IOException {
		List<String> errors = compareObjects(object);

		if (!errors.isEmpty()) {
			throw new RuntimeException(errors.stream().collect(Collectors.joining("\n")));
		}
	}

	protected static <T> List<String> compareObjects(T object) {
		Object result = serializeAndBack(object, object.getClass());
		List<String> errors = compareObjects(object, result);
		debugOut.println("errors:\n" + errors);
		return errors;
	}

	protected static Object serializeAndBack(Object object, Class<?> clazz) {
		String json = serialize(object);
		debugOut.println("serialized:\n" + json);
		Object deserialized = deserialize(clazz, json);
		debugOut.println("deserialized:\n" + serialize(deserialized));
		return deserialized;
	}

	private static Object deserialize(Class<?> clazz, String json) {
		try {
			return mapper.readerFor(clazz).readValue(json);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String serialize(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static <T> List<String> compareObjects(T before, Object after) {
		Map<String, Object> beforeValues = getAllJsonPropertyValues(before, before.getClass());
		Map<String, Object> afterValues = getAllJsonPropertyValues(after, after.getClass());

		HashSet<String> union = new HashSet<>();
		union.addAll(beforeValues.keySet());
		union.addAll(afterValues.keySet());

		List<String> errors = new ArrayList<>();
		union.forEach(key -> collectErrors(key, beforeValues, afterValues, errors));
		return errors;
	}

	private static void collectErrors(String key, Map<String, Object> beforeValues, Map<String, Object> afterValues,
			List<String> errors) {
		boolean inBefore = beforeValues.containsKey(key);
		boolean inAfter = afterValues.containsKey(key);

		if (inBefore && inAfter) {
			equalsJsonObject(key, beforeValues.get(key), afterValues.get(key), errors);
		} else if (inBefore && !inAfter) {
			errors.add(String.format("%s does not exist anymore", key));
		} else {
			// !inBefore && inAfter
			errors.add(String.format("%s did not exist before", key));
		}
	}

	private static void equalsJsonObject(Object key, Object before, Object after, List<String> errors) {
		if (before == null ^ after == null) {
			errors.add(
					String.format("%s is %snull but was %snull", key, before == null ? "" : "not ", after == null ? "" : "not "));
			return;
		}

		if (before == null && after == null) {
			return;
		}

		if (before.getClass().isArray() && after.getClass().isArray()) {
			compareArrays(key, before, after, errors);
			return;
		}

		if (Collection.class.isAssignableFrom(before.getClass()) && Collection.class.isAssignableFrom(after.getClass())) {
			compareCollections(key, (Collection<?>) before, (Collection<?>) after, errors);
			return;
		}

		unknownObjectEquals(key, before, after, errors);
	}

	private static void compareCollections(Object key, Collection<?> before, Collection<?> after, List<String> errors) {
		int lenBefore = before.size();
		int lenAfter = after.size();

		if (lenBefore != lenAfter) {
			errors.add(String.format("'%s' collection length was %d is %d", key, lenBefore, lenAfter));
			return;
		}

		Iterator<?> beforeIt = before.iterator();
		Iterator<?> afterIt = after.iterator();

		boolean equal = true;
		while (beforeIt.hasNext()) {
			Object b = beforeIt.next();
			Object a = afterIt.next();

			if (a == null && b == null) {
				continue;
			} else if (a == null ^ b == null) {
				equal = false;
				break;
			} else if (!unknownObjectEquals(key + "[?]", b, a, errors)) {
				equal = false;
				break;
			}
		}

		if (!equal) {
			errors.add(String.format("'%s' collections do not match", key));
		}
	}

	private static void compareArrays(Object key, Object before, Object after, List<String> errors) {
		int lenBefore = Array.getLength(before);
		int lenAfter = Array.getLength(after);

		if (lenBefore != lenAfter) {
			errors.add(String.format("%s array length was %d is %d", key, lenBefore, lenAfter));
			return;
		}

		boolean equal = true;
		for (int i = 0; i < lenBefore; i++) {
			Object a = Array.get(before, i);
			Object b = Array.get(after, i);

			if (a == null && b == null) {
				continue;
			} else if (a == null ^ b == null) {
				equal = false;
				break;
			} else if (!unknownObjectEquals(key + "[" + i + "]", b, a, errors)) {
				equal = false;
				break;
			}
		}

		if (!equal) {
			errors.add(String.format("%s arrays do not match", key));
		}
	}

	private static boolean unknownObjectEquals(Object key, Object before, Object after, List<String> errors) {
		boolean hasJsonPropertyValues = hasJsonPropertyValues(before, before.getClass());

		if (hasJsonPropertyValues) {
			List<String> subErrors = compareObjects(before, after);
			if (subErrors.size() > 0) {
				errors.addAll(subErrors);
				errors.add(String.format("%s was '%s' is '%s'", key, before, after));
				return false;
			}
			return true;
		} else {
			boolean equals = after.equals(before);
			if (!equals) {
				errors.add(String.format("%s was '%s' is '%s'", key, before, after));
			}
			return equals;
		}
	}

	private static <A extends Annotation> boolean hasJsonPropertyValues(Object instance, Class<?> clazz) {
		return !getAllJsonPropertyValues(instance, clazz).isEmpty();
	}

	private static <A extends Annotation> Map<String, Object> getAllJsonPropertyValues(Object instance, Class<?> clazz) {
		Map<String, Object> properties = new HashMap<>();
		ReflectionUtils.doWithFields(
				clazz,
				field -> {
					field.setAccessible(true);
					properties.put(
							field.getName(),
							field.get(instance));
				},
				field -> AnnotationUtils.getAnnotation(field, JsonProperty.class) != null);
		return properties;
	}

}