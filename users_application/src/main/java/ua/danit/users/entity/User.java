package ua.danit.users.entity;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {
  @Id
  private String login;
  private String password;
  private String username;
  @Column(name = "registration_date")
  private Timestamp registrationDate;

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Timestamp getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Timestamp registrationDate) {
    this.registrationDate = registrationDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    User user = (User) o;

    if (login != null ? !login.equals(user.login) : user.login != null) {
      return false;
    }
    if (password != null ? !password.equals(user.password) : user.password != null) {
      return false;
    }
    if (username != null ? !username.equals(user.username) : user.username != null) {
      return false;
    }
    return registrationDate != null ? registrationDate.equals(user.registrationDate) :
        user.registrationDate == null;
  }

  @Override
  public int hashCode() {
    int result = login != null ? login.hashCode() : 0;
    result = 31 * result + (password != null ? password.hashCode() : 0);
    result = 31 * result + (username != null ? username.hashCode() : 0);
    result = 31 * result + (registrationDate != null ? registrationDate.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "User{" + "login='" + login + '\'' + ", password='" + password + '\'' + ", username='"
           + username + '\'' + ", registrationDate=" + registrationDate + '}';
  }
}
