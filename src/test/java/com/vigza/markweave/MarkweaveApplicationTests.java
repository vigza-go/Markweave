package com.vigza.markweave;

import com.vigza.markweave.common.util.TextOperation;
import com.vigza.markweave.infrastructure.persistence.entity.User;
import com.vigza.markweave.infrastructure.persistence.mapper.UserMapper;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
class MarkweaveApplicationTests {

	// @Autowired
	// private UserMapper userMapper;

	@Test
	void contextLoads() {
	}

	@Test
	public void test(){
			// console.log("start") 
            // let baseText = "hello?hello"
            // let op1 = ot.TextOperation().retain(4).delete(3).retain(4);
            // let opb1 = ot.TextOperation().retain(6).insert("1").retain(5);
            // console.log("opb1: ", opb1.toJSON())
            // let transformed = ot.TextOperation.transform(op1, opb1);
            // console.log("transformed: ", transformed[1].toJSON())
            // let opb2 = ot.TextOperation().retain(7).insert("2").retain(5);
            // console.log("opb2: ", opb2.toJSON())
            // console.log("text" ,opb1.compose(opb2).apply(baseText))
            // console.log("op11: ", transformed[0].toJSON())
            // let transformed2 = ot.TextOperation.transform(transformed[0], opb2);
            // console.log("transformed2: ", transformed2[1].toJSON())
		// String baseText = "hello?hello";
		// TextOperation op1 = new TextOperation().retain(4).delete(3).retain(4);
		// TextOperation opb1 = new TextOperation().retain(6).insert("1").retain(5);
		// TextOperation opb2 = new TextOperation().retain(7).insert("2").retain(5);
		// System.out.println(opb2.toJSON());
		String json = new String("{\"version\": 123,\"op\": [7,\"2\",5]}");
		JSONObject jsonObj = JSONUtil.parseObj(json);
		System.out.println(jsonObj.getStr("op"));
	}

}
