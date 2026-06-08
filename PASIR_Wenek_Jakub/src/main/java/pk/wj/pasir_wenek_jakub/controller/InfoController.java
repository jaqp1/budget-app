package pk.wj.pasir_wenek_jakub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InfoController {

    @GetMapping("/info")
    public Map<String, String> getAppInfo(){
        Map<String, String> info = new HashMap<>();

        info.put("appName", "Aplikacja Budżetowa");
        info.put("version","1.0");
        info.put("message", "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!");

        return info;
    }

}
