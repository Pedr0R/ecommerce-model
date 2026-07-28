package p.ramos.ms.users;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @ResponseBody
    @GetMapping("/users")
    public List<UserModel> getUsers() {
    
    }
}
