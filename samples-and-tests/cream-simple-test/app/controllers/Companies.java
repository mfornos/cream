package controllers;

import models.Company;
import play.modules.cream.annotations.JcrSession;

@JcrSession
@CRUD.For(Company.class)
public class Companies extends CRUD {

}
