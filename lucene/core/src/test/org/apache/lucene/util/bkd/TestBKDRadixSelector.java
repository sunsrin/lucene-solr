/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.util.bkd;


import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FutureArrays;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.TestUtil;

public class TestBKDRadixSelector extends LuceneTestCase {

  public void testBasic() throws IOException {
    int values = 4;
    Directory dir = getDirectory(values);
    int middle = 2;
    int dimensions =1;
    int bytesPerDimensions = Integer.BYTES;
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    byte[] bytes = new byte[Integer.BYTES];
    NumericUtils.intToSortableBytes(1, bytes, 0);
    points.append(bytes, 0);
    NumericUtils.intToSortableBytes(2, bytes, 0);
    points.append(bytes, 1);
    NumericUtils.intToSortableBytes(3, bytes, 0);
    points.append(bytes, 2);
    NumericUtils.intToSortableBytes(4, bytes, 0);
    points.append(bytes, 3);
    points.close();
    verify(dir, points, dimensions, 0, values, middle, packedLength, bytesPerDimensions, 0);
    dir.close();
  }

  public void testRandomBinaryTiny() throws Exception {
    doTestRandomBinary(10);
  }

  public void testRandomBinaryMedium() throws Exception {
    doTestRandomBinary(25000);
  }

  @Nightly
  public void testRandomBinaryBig() throws Exception {
    doTestRandomBinary(500000);
  }

  private void doTestRandomBinary(int count) throws IOException {
    int values = TestUtil.nextInt(random(), count, count*2);
    Directory dir = getDirectory(values);
    int start;
    int end;
    if (random().nextBoolean()) {
      start = 0;
      end = values;
    } else  {
      start = TestUtil.nextInt(random(), 0, values -3);
      end = TestUtil.nextInt(random(), start  + 2, values);
    }
    int partitionPoint = TestUtil.nextInt(random(), start + 1, end - 1);
    int sortedOnHeap = random().nextInt(5000);
    int dimensions =  TestUtil.nextInt(random(), 1, 8);
    int bytesPerDimensions = TestUtil.nextInt(random(), 2, 30);
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    byte[] value = new byte[packedLength];
    for (int i =0; i < values; i++) {
      random().nextBytes(value);
      points.append(value, i);
    }
    points.close();
    verify(dir, points, dimensions, start, end, partitionPoint, packedLength, bytesPerDimensions, sortedOnHeap);
    dir.close();
  }

  public void testRandomAllDimensionsEquals() throws IOException {
    int values =  TestUtil.nextInt(random(), 15000, 20000);
    Directory dir = getDirectory(values);
    int partitionPoint = random().nextInt(values);
    int sortedOnHeap = random().nextInt(5000);
    int dimensions =  TestUtil.nextInt(random(), 1, 8);
    int bytesPerDimensions = TestUtil.nextInt(random(), 2, 30);
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    byte[] value = new byte[packedLength];
    random().nextBytes(value);
    for (int i =0; i < values; i++) {
      if (random().nextBoolean()) {
        points.append(value, i);
      } else {
        points.append(value, random().nextInt(values));
      }
    }
    points.close();
    verify(dir, points, dimensions, 0, values, partitionPoint, packedLength, bytesPerDimensions, sortedOnHeap);
    dir.close();
  }

  public void testRandomLastByteTwoValues() throws IOException {
    int values = random().nextInt(15000) + 1;
    Directory dir = getDirectory(values);
    int partitionPoint = random().nextInt(values);
    int sortedOnHeap = random().nextInt(5000);
    int dimensions =  TestUtil.nextInt(random(), 1, 8);
    int bytesPerDimensions = TestUtil.nextInt(random(), 2, 30);
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    byte[] value = new byte[packedLength];
    random().nextBytes(value);
    for (int i =0; i < values; i++) {
      if (random().nextBoolean()) {
        points.append(value, 1);
      } else {
        points.append(value, 2);
      }
    }
    points.close();
    verify(dir, points, dimensions, 0, values, partitionPoint, packedLength, bytesPerDimensions, sortedOnHeap);
    dir.close();
  }

  public void testRandomAllDocsEquals() throws IOException {
    int values = random().nextInt(15000) + 1;
    Directory dir = getDirectory(values);
    int partitionPoint = random().nextInt(values);
    int sortedOnHeap = random().nextInt(5000);
    int dimensions =  TestUtil.nextInt(random(), 1, 8);
    int bytesPerDimensions = TestUtil.nextInt(random(), 2, 30);
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    byte[] value = new byte[packedLength];
    random().nextBytes(value);
    for (int i =0; i < values; i++) {
      points.append(value, 0);
    }
    points.close();
    verify(dir, points, dimensions, 0, values, partitionPoint, packedLength, bytesPerDimensions, sortedOnHeap);
    dir.close();
  }

  public void testRandomFewDifferentValues() throws IOException {
    int values = atLeast(15000);
    Directory dir = getDirectory(values);
    int partitionPoint = random().nextInt(values);
    int sortedOnHeap = random().nextInt(5000);
    int dimensions =  TestUtil.nextInt(random(), 1, 8);
    int bytesPerDimensions = TestUtil.nextInt(random(), 2, 30);
    int packedLength = dimensions * bytesPerDimensions;
    PointWriter points = getRandomPointWriter(dir, values, packedLength);
    int numberValues = random().nextInt(8) + 2;
    byte[][] differentValues = new byte[numberValues][packedLength];
    for (int i =0; i < numberValues; i++) {
      random().nextBytes(differentValues[i]);
    }
    for (int i =0; i < values; i++) {
      points.append(differentValues[random().nextInt(numberValues)], i);
    }
    points.close();
    verify(dir, points, dimensions, 0, values, partitionPoint, packedLength, bytesPerDimensions, sortedOnHeap);
    dir.close();
  }

  private void verify(Directory dir, PointWriter points, int dimensions, long start, long end, long middle, int packedLength, int bytesPerDimensions, int sortedOnHeap) throws IOException{
    for (int splitDim =0; splitDim < dimensions; splitDim++) {
      PointWriter copy = copyPoints(dir, points, packedLength);
      PointWriter leftPointWriter = getRandomPointWriter(dir, middle - start, packedLength);
      PointWriter rightPointWriter = getRandomPointWriter(dir, end - middle, packedLength);
      BKDRadixSelector radixSelector = new BKDRadixSelector(dimensions, bytesPerDimensions, sortedOnHeap, dir, "test");
      byte[] partitionPoint = radixSelector.select(copy, leftPointWriter, rightPointWriter, start, end, middle, splitDim);
      leftPointWriter.close();
      rightPointWriter.close();
      byte[] max = getMax(leftPointWriter, middle - start, bytesPerDimensions, splitDim);
      byte[] min = getMin(rightPointWriter, end - middle, bytesPerDimensions, splitDim);
      int cmp = FutureArrays.compareUnsigned(max, 0, bytesPerDimensions, min, 0, bytesPerDimensions);
      assertTrue(cmp <= 0);
      if (cmp == 0) {
        int maxDocID = getMaxDocId(leftPointWriter, middle - start, bytesPerDimensions, splitDim, partitionPoint);
        int minDocId = getMinDocId(rightPointWriter, end - middle, bytesPerDimensions, splitDim, partitionPoint);
        assertTrue(minDocId >= maxDocID);
      }
      assertTrue(Arrays.equals(partitionPoint, min));
      leftPointWriter.destroy();
      rightPointWriter.destroy();
    }
    points.destroy();
  }

  private PointWriter copyPoints(Directory dir, PointWriter points, int packedLength) throws IOException {
    BytesRef bytesRef = new BytesRef();

    try (PointWriter copy  = getRandomPointWriter(dir, points.count(), packedLength);
         PointReader reader = points.getReader(0, points.count())) {
      while (reader.next()) {
        reader.packedValue(bytesRef);
        copy.append(bytesRef, reader.docID());
      }
      return copy;
    }
  }

  private PointWriter getRandomPointWriter(Directory dir, long numPoints, int packedBytesLength) throws IOException {
    if (numPoints < 4096 && random().nextBoolean()) {
      return new HeapPointWriter(Math.toIntExact(numPoints), Math.toIntExact(numPoints), packedBytesLength);
    } else {
      return new OfflinePointWriter(dir, "test", packedBytesLength, "data", numPoints);
    }
  }

  private Directory getDirectory(int numPoints) {
    Directory dir;
    if (numPoints > 100000) {
      dir = newFSDirectory(createTempDir("TestBKDTRadixSelector"));
    } else {
      dir = newDirectory();
    }
    return dir;
  }

  private byte[] getMin(PointWriter p, long size, int bytesPerDimension, int dimension) throws  IOException {
    byte[] min = new byte[bytesPerDimension];
    Arrays.fill(min, (byte) 0xff);
    try (PointReader reader = p.getReader(0, size)) {
      byte[] value = new byte[bytesPerDimension];
      BytesRef packedValue = new BytesRef();
      while (reader.next()) {
        reader.packedValue(packedValue);
        System.arraycopy(packedValue.bytes, packedValue.offset + dimension * bytesPerDimension, value, 0, bytesPerDimension);
        if (FutureArrays.compareUnsigned(min, 0, bytesPerDimension, value, 0, bytesPerDimension) > 0) {
          System.arraycopy(value, 0, min, 0, bytesPerDimension);
        }
      }
    }
    return min;
  }

  private int getMinDocId(PointWriter p, long size, int bytesPerDimension, int dimension, byte[] partitionPoint) throws  IOException {
   int docID = Integer.MAX_VALUE;
    try (PointReader reader = p.getReader(0, size)) {
      BytesRef packedValue = new BytesRef();
      while (reader.next()) {
        reader.packedValue(packedValue);
        int offset = dimension * bytesPerDimension;
        if (FutureArrays.compareUnsigned(packedValue.bytes, packedValue.offset + offset, packedValue.offset + offset + bytesPerDimension, partitionPoint, 0, bytesPerDimension) == 0) {
          int newDocID = reader.docID();
          if (newDocID < docID) {
            docID = newDocID;
          }
        }
      }
    }
    return docID;
  }

  private byte[] getMax(PointWriter p, long size, int bytesPerDimension, int dimension) throws  IOException {
    byte[] max = new byte[bytesPerDimension];
    Arrays.fill(max, (byte) 0);
    try (PointReader reader = p.getReader(0, size)) {
      byte[] value = new byte[bytesPerDimension];
      BytesRef packedValue = new BytesRef();
      while (reader.next()) {
        reader.packedValue(packedValue);
        System.arraycopy(packedValue.bytes, packedValue.offset + dimension * bytesPerDimension, value, 0, bytesPerDimension);
        if (FutureArrays.compareUnsigned(max, 0, bytesPerDimension, value, 0, bytesPerDimension) < 0) {
          System.arraycopy(value, 0, max, 0, bytesPerDimension);
        }
      }
    }
    return max;
  }

  private int getMaxDocId(PointWriter p, long size, int bytesPerDimension, int dimension, byte[] partitionPoint) throws  IOException {
    int docID = Integer.MIN_VALUE;
    try (PointReader reader = p.getReader(0, size)) {
      BytesRef packedValue = new BytesRef();
      while (reader.next()) {
        reader.packedValue(packedValue);
        int offset = dimension * bytesPerDimension;
        if (FutureArrays.compareUnsigned(packedValue.bytes, packedValue.offset + offset, packedValue.offset + offset + bytesPerDimension, partitionPoint, 0, bytesPerDimension) == 0) {
          int newDocID = reader.docID();
          if (newDocID > docID) {
            docID = newDocID;
          }
        }
      }
    }
    return docID;
  }

}
