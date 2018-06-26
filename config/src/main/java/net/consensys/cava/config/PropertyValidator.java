/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.config;

import static net.consensys.cava.config.ConfigurationErrors.noErrors;
import static net.consensys.cava.config.ConfigurationErrors.singleError;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A validator associated with a specific configuration property.
 */
public interface PropertyValidator<T> {

  /**
   * Returns a single validator that combines the results of several validators.
   *
   * @param first The first validator.
   * @param second The second validator.
   * @param <T> The validator type.
   * @return A single validator that combines the results of evaluating the provided validators.
   */
  static <T> PropertyValidator<T> combine(PropertyValidator<? super T> first, PropertyValidator<? super T> second) {
    return combine(Arrays.<PropertyValidator<? super T>>asList(first, second));
  }

  /**
   * Returns a single validator that combines the results of several validators.
   *
   * @param validators The validators to be evaluated.
   * @param <T> The validator type.
   * @return A single validator that combines the results of evaluating the provided validators.
   */
  static <T> PropertyValidator<T> combine(List<PropertyValidator<? super T>> validators) {
    return (key, position, value) -> validators
        .stream()
        .flatMap(validator -> validator.validate(key, position, value).stream())
        .collect(Collectors.toList());
  }


  /**
   * A validator that applies a validator to all elements of list, if the list is present.
   *
   * @param validator The validator to apply to elements of the list.
   * @param <T> The type of list elements.
   * @return A validator that applies a validator to all elements of list, if the list is present.
   */
  static <T> PropertyValidator<List<T>> allInList(PropertyValidator<? super T> validator) {
    return (key, position, value) -> {
      if (value != null) {
        return value.stream().flatMap(elem -> validator.validate(key, position, elem).stream()).collect(
            Collectors.toList());
      }
      return noErrors();
    };
  }

  /**
   * A validator that ensures a property is present.
   *
   * @return A validator that ensures a property is present.
   */
  static PropertyValidator<Object> isPresent() {
    return PropertyValidators.IS_PRESENT;
  }

  /**
   * A validator that ensures a property, if present, is within a long integer range.
   *
   * @param from The lower bound of the range (inclusive).
   * @param to The upper bound of the range (exclusive).
   * @return A validator that ensures a property, if present, is within an integer range.
   */
  static PropertyValidator<Number> inRange(long from, long to) {
    return (key, position, value) -> {
      if (value != null && (value.longValue() < from || value.longValue() >= to)) {
        return singleError(position, "Value of property '" + key + "' is outside range [" + from + "," + to + ")");
      }
      return noErrors();
    };
  }

  /**
   * A validator that ensures a property, if present, has a value within a given set.
   *
   * @param values The acceptable values.
   * @return A validator that ensures a property, if present, is within a given set.
   */
  static PropertyValidator<String> anyOf(String... values) {
    return anyOf(Arrays.asList(values));
  }

  /**
   * A validator that ensures a property, if present, has a value within a given set.
   *
   * @param values The acceptable values.
   * @return A validator that ensures a property, if present, is within a given set.
   */
  static PropertyValidator<String> anyOf(Collection<String> values) {
    StringBuilder builder = new StringBuilder();
    int count = values.size();
    int i = 0;
    for (String value : values) {
      builder.append('"');
      builder.append(value);
      builder.append('"');
      if (i < (count - 2)) {
        builder.append(", ");
      } else if (i == (count - 2)) {
        if (count >= 3) {
          builder.append(',');
        }
        builder.append(" or ");
      }
      ++i;
    }
    String expected = builder.toString();
    return (key, position, value) -> {
      if (value != null && !values.contains(value)) {
        return singleError(position, "Value of property '" + key + "' should be " + expected);
      }
      return noErrors();
    };
  }

  /**
   * A validator that ensures a property, if present, is a well-formed URL.
   *
   * @return A validator that ensures a property, if present, is a well-formed URL.
   */
  static PropertyValidator<String> isURL() {
    return (key, position, value) -> {
      if (value != null) {
        try {
          new URL(value);
        } catch (MalformedURLException e) {
          return singleError(position, "Value of property '" + key + "' is not a valid URL", e);
        }
      }
      return noErrors();
    };
  }

  /**
   * Validate a configuration property.
   *
   * @param key The configuration property key.
   * @param position The position of the property in the input document, if supported. This should be used when
   *        constructing errors.
   * @param value The value associated with the configuration entry.
   * @return A list of errors. If no errors are found, an empty list should be returned.
   */
  List<ConfigurationError> validate(String key, @Nullable DocumentPosition position, @Nullable T value);
}
