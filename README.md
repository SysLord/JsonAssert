# JsonAssert

## Idea

Automatically compare a given instance of a json serializable class with the serialized and deserialized instance. This prevents nasty surprises when used in a closed service environment (class is used for serialization and deserialization and given instance is assumed to be the same).

I don't know if there's already something better than this.

## Usage
```java
JsonAssert.assertEqualUsingJsonProperty(<instance>);
```


## Known Drawbacks
 * If a field is not comparable by equals it cannot be compared successfully, so it needs to be null.
 
