package org.apache.dubbo.admin.common.utils;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author frank woo(吴峻申) <br>
 *     email:<a href="mailto:frank_wjs@hotmail.com">frank_wjs@hotmail.com</a> <br>
 * @date 2020/3/3 2:17 下午 <br>
 */
public class Sha256UtilsTest {

  @Test
  public void getSHA256() {
    assertNull(Sha256Utils.getSha256(null));

    String input = "admin123";
    String output = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    assertThat(Sha256Utils.getSha256(input), equalTo(output));

    input = "admin123!@#";
    output = "0336b1504c54b042bd75e65e96ca555d51d7a949251752577a35aea47c1705ee";
    assertThat(Sha256Utils.getSha256(input), equalTo(output));
  }
}
