/*
 * Copyright 2018 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.ab.mt6735scatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

  public static final String FOLDER_NAME = "raw.partitions";

  public static void main(String[] args) throws IOException {
    final Scanner scanner = new Scanner(new File(args[0]));
    final StringBuilder stringBuilder = new StringBuilder();
    FileInputStream fis = new FileInputStream(new File(args[1]));
    FileChannel input = fis.getChannel();

    String partitionName = "";
    long linearStartAddr = 0;
    long partitionSize = 0;
    boolean notSkipThis = false;
    String fileName = "";

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();

      if (line.trim().startsWith("region:")) notSkipThis = "EMMC_USER".equals(StringUtils.substringAfter(line, ":").trim());

      if (line.trim().startsWith("partition_name:")) partitionName = StringUtils.substringAfter(line, ":").trim();
      if (line.trim().startsWith("linear_start_addr:")) {
        linearStartAddr = Long.decode(StringUtils.substringAfter(line, ":").trim());
        input.position(linearStartAddr);
      }
      if (line.trim().startsWith("physical_start_addr:") && (linearStartAddr != Long.decode(StringUtils.substringAfter(line, ":").trim())))
          throw new ArithmeticException();

      if (line.trim().startsWith("file_name:")) {
        fileName = StringUtils.substringAfter(line, ":").trim();
        if ("NONE".equals(fileName)) {
          fileName = partitionName + ".img";
          line = StringUtils.replace(line, "NONE", fileName);
        }
      }

      if (line.trim().startsWith("file_name:")) {
        line = StringUtils.replace(line, "NONE", fileName);
      }

      if (line.trim().startsWith("partition_size:")) {
        partitionSize = Long.decode(StringUtils.substringAfter(line, ":").trim());
      }

      if (line.trim().startsWith("reserve:") && notSkipThis) {
        System.out.println(partitionName + " " + linearStartAddr + " " + partitionSize);

        try (FileOutputStream fos = new FileOutputStream(new File(FOLDER_NAME + "/" + fileName)); FileChannel output = fos.getChannel()) {
          long srcLen = partitionSize;
          long dstLen = 0L;
          long bytesCopied;
          for(long count = 0L; dstLen < srcLen; dstLen += bytesCopied) {
            long remain = srcLen - dstLen;
            count = remain > 31457280L ? 31457280L : remain;
            bytesCopied = output.transferFrom(input, dstLen, count);
            if (bytesCopied == 0L) {
              break;
            }
          }
        }

      }

      stringBuilder.append(line).append('\n');
    }
    FileUtils.writeByteArrayToFile(new File(FOLDER_NAME + "/scatter.txt"),
        stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
  }

}
