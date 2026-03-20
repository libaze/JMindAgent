package com.itheima.jmindagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.itheima.jmindagent.mapper")
public class JMindAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(JMindAgentApplication.class, args);
	}

}
