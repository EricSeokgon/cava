/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.cava.bytes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

import java.util.Arrays;

class MutableArrayWrappingBytes extends ArrayWrappingBytes implements MutableBytes {

  MutableArrayWrappingBytes(byte[] bytes) {
    super(bytes);
  }

  MutableArrayWrappingBytes(byte[] bytes, int offset, int length) {
    super(bytes, offset, length);
  }

  @Override
  public void set(int i, byte b) {
    // Check bounds because while the array access would throw, the error message would be confusing
    // for the caller.
    checkElementIndex(i, size());
    this.bytes[offset + i] = b;
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    if (i == 0 && length == this.length)
      return this;
    if (length == 0)
      return MutableBytes.EMPTY;

    checkElementIndex(i, this.length);
    checkArgument(
        i + length <= this.length,
        "Specified length %s is too large: the value has size %s and has only %s bytes from %s",
        length,
        this.length,
        this.length - i,
        i);
    return length == Bytes32.SIZE ? new MutableArrayWrappingBytes32(bytes, offset + i)
        : new MutableArrayWrappingBytes(bytes, offset + i, length);
  }

  @Override
  public void fill(byte b) {
    Arrays.fill(bytes, offset, offset + length, b);
  }

  @Override
  public Bytes copy() {
    return new ArrayWrappingBytes(toArray());
  }
}
