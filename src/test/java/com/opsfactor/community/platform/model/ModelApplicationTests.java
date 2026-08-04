package com.opsfactor.community.platform.model;

import com.opsfactor.community.bootstrap.CommunityModelApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

//@RunWith(SpringRunner.class) deprecated, junit4
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CommunityModelApplication.class)
public class ModelApplicationTests {
    
    @Test
    public void contextLoads() {
    }
    
}
