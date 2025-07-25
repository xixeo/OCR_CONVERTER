package org.test.ocrconverter.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Controller
public class FileUploadController {

    @GetMapping("/")
    public String uploadPage(@RequestParam(value = "success", required = false) String success, Model model) {
        if ("true".equals(success)) {
            model.addAttribute("message", "파일이 성공적으로 업로드되었습니다!");
        }
        return "upload";
    }


}
