package com.katia.shop.user.exception;

public class EmailAlreadyExistException extends RuntimeException{
  public EmailAlreadyExistException(String email){
      super("This email : " + email + " already exists!");
  }
}
