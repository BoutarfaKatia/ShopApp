package com.katia.shop;

import org.springframework.boot.SpringApplication;
import com.katia.shop.support.TestcontainersConfiguration;

public class TestShopApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShopApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
