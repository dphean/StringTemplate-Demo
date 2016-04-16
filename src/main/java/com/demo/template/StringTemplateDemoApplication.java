package com.demo.template;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.NumberRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

@SpringBootApplication
public class StringTemplateDemoApplication {
  Logger logger = LoggerFactory.getLogger(StringTemplateDemoApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(StringTemplateDemoApplication.class, args);
  }

  @PostConstruct
  public void demo() {
    ST hello = new ST("Hello, <name>");
    hello.add("name", "World");
    logger.info(hello.render());


    STGroup group = new STGroupDir("template");
    ST st = group.getInstanceOf("decl");
    st.add("type", "int");
    st.add("name", "x");
    st.add("value", 0);
    String result = st.render(); // yields "int x = 0;"
    logger.info(result);



    group = new STGroupFile("template/test.stg");
    st = group.getInstanceOf("decl");
    st.add("type", "int");
    st.add("name", "x");
    st.add("value", 0);
    result = st.render(); // yields "int x = 0;"
    logger.info(result);



    st = new ST("<b>$u.id$</b>: $u.name$", '$', '$');
    st.add("u", new User(999, "parrt"));
    result = st.render(); // "<b>999</b>: parrt"
    logger.info(result);



    st = new ST("<items:{it|<it.id>: <it.lastName>, <it.firstName>\n}>");
    st.addAggr("items.{ firstName ,lastName, id }", "Ter", "Parr", 99); // add() uses varargs
    st.addAggr("items.{firstName, lastName ,id}", "Tom", "Burns", 34);
    logger.info(st.render());


    int[] num =
        new int[] {3,9,20,2,1,4,6,32,5,6,77,888,2,1,6,32,5,6,77,
        4,9,20,2,1,4,63,9,20,2,1,4,6,32,5,6,77,6,32,5,6,77,
        3,9,20,2,1,4,6,32,5,6,77,888,1,6,32,5};
    String t =
        ST.format(30, "int <%1>[] = { <%2; wrap, anchor, separator=\", \"> };", "a", num);
    logger.info(t);



    String template = "foo(x) ::= \"<x.id>: <x.name>\"\n";
    STGroup g = new STGroupString(template);
    g.registerModelAdaptor(UserB.class, new UserAdaptor());
    st = g.getInstanceOf("foo");
    st.add("x", new UserB(100, "parrt"));
    String expecting = "100: parrt";
    result = st.render();
    logger.info(result);
    
    
    
    template = "foo(x) ::= \"<x.id>: <x.name> (<x.description>)\"\n";
    g = new STGroupString(template);
    g.registerModelAdaptor(UserC.class, new UserAdaptorC());
    st = g.getInstanceOf("foo");
    st.add("x", new UserC(100, "parrt"));
    expecting = "100: Parrt (User object with id:100)";
    result = st.render();
    logger.info(result);
    
    
    // Template regions
    group = new STGroupFile("template/Dbg.stg");
    st = group.getInstanceOf("method");
    st.add("name", "int");
    st.add("code", "x");
    result = st.render(); // yields "int x = 0;"
    logger.info(result);
    
    
    //Replace a region of existing template text
    group = new STGroupFile("template/Dbg.stg");
    st = group.getInstanceOf("test");
    st.add("expr", "int");
    st.add("code", "x");
    result = st.render(); // yields "int x = 0;"
    logger.info(result);
    
    // Attribute Renderers
    template =
        "foo(x,y) ::= << <x; format=\"%,d\"> <y; format=\"%,2.3f\"> >>\n";
    g = new STGroupString(template);
    g.registerRenderer(Number.class, new NumberRenderer());
    st = g.getInstanceOf("foo");
    st.add("x", -2100);
    st.add("y", 3.14159);
    result = st.render(new Locale("pl"));
    // resulted is " -2 100 3,142 " since Polish uses ' ' for ',' and ',' for '.'
    logger.info(result);
    

  }

  public static class User {
    public int id; // template can directly access via u.id
    private String name; // template can't access this
    public User(int id, String name) { this.id = id; this.name = name; }
    public boolean isManager() { return true; } // u.manager
    public boolean hasParkingSpot() { return true; } // u.parkingSpot
    public String getName() { return name; } // u.name
    public String toString() { return id+":"+name; } // u
  }


  class UserAdaptor implements ModelAdaptor {
    @Override
    public Object getProperty(Interpreter interpreter, ST self, Object o, Object property, String propertyName)
        throws STNoSuchPropertyException
    {
      if ( propertyName.equals("id") ) return ((UserB)o).id;
      if ( propertyName.equals("name") ) return ((UserB)o).theName();
      throw new STNoSuchPropertyException(null, "User."+propertyName, propertyName);
    }
  }

  public static class UserB {
    private int id; // ST can't see; it's private
    private String name;
    public UserB(int id, String name) { this.id = id; this.name = name; }
    public String theName() { return name; } // doesn't follow naming conventions
  }


  class UserAdaptorC extends ObjectModelAdaptor {
    public Object getProperty(Interpreter interpreter, ST self, Object o, Object property, String propertyName)
        throws STNoSuchPropertyException
    {
      // intercept handling of "name" property and capitalize first character
      if ( propertyName.equals("name") ) return ((UserC)o).name.substring(0,1).toUpperCase()+((UserC)o).name.substring(1);
      // respond to "description" property by composing desired result
      if ( propertyName.equals("description") ) return "User object with id:" + ((UserC)o).id;
      // let "id" be handled by ObjectModelAdaptor
      return super.getProperty(interpreter,self,o,property,propertyName);
    }
  }

  public static class UserC {
    public int id; // ST can see this and we'll let ObjectModelAdaptor handle it
    public String name;  // ST can see this, but we'll override to capitalize
    public UserC(int id, String name) { this.id = id; this.name = name; }
  }
}
