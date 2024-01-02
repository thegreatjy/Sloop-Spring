package kr.co.sloop.postForum.controller;

import kr.co.sloop.postForum.domain.PostForumDTO;
import kr.co.sloop.postForum.service.PostForumServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

@Log4j
@Controller
@RequestMapping("/postforum")
@RequiredArgsConstructor
public class PostForumController {
    @Resource(name="uploadPath")
    private String uploadPath;
    private final PostForumServiceImpl postForumServiceImpl;

    // 글 작성하기 : 화면 출력
    @GetMapping("/write")
    public String writeForm(Model model){
        PostForumDTO postForumDTO = new PostForumDTO();
        postForumDTO.setCategoryPostIdx(1);
        model.addAttribute("postForumDTO", postForumDTO);
        return "postForum/write";
    }

    // 글 작성하기
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("postForumDTO") PostForumDTO postForumDTO, BindingResult errors, HttpSession session){
        log.info(errors);
        log.info(postForumDTO);

        // 객체 바인딩에 유효성 오류가 존재한다면, 작성 페이지로 돌아가서 오류 메세지를 출력한다.
        if(errors.hasErrors()){
            return "postForum/write";
        }

        // 로그인 되어 있는 사용자 email을 세션에서 가져온다. [*****]
        // String memberEmail = (String) session.getAttribute("memberEmail");
        String memberEmail = "test@test.com";
        postForumDTO.setMemberEmail(memberEmail);

        // 게시판 idx(boardIdx)를 쿼리 스트링을 통해 가져와야 한다. [*****]
        // @RequestParam("boardIdx") int boardIdx
        int boardIdx = 3;
        postForumDTO.setBoardIdx(3);

        boolean result = postForumServiceImpl.write(postForumDTO);

        if(result){ // 글 작성 성공
            // 해당 글 상세 조회 페이지로 이동
            return "redirect:/postforum/detail?postIdx=" + postForumDTO.getPostIdx();
        }else {     // 글 작성 실패
            return "postForum/write";
        }
    }

    // 글 작성하기 : 이미지 업로드
    @PostMapping("/upload-image")
    public void imageUpload(HttpServletRequest request,
                            HttpServletResponse response, MultipartHttpServletRequest multiFile
            , @RequestParam MultipartFile upload) throws Exception {
        // 랜덤 문자 생성
        UUID uid = UUID.randomUUID();

        OutputStream out = null;
        PrintWriter printWriter = null;

        //인코딩
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        try {
            //파일 이름 가져오기
            String fileName = upload.getOriginalFilename();
            byte[] bytes = upload.getBytes();

            //이미지 경로 생성
            log.info("\n\n ===== 현재 경로 : " + request.getContextPath());
            String path = "/resources/uploads/";    // 이미지 경로 설정(폴더 자동 생성)

            String ckUploadPath = path + uid + "_" + fileName;
            ckUploadPath = uploadPath + File.separator + "uploads" + File.separator + uid + "_" + fileName;
            log.info("uploadPath : " + uploadPath);
            log.info("ckUploadPath : " + ckUploadPath);

            File folder = new File(path);
            log.info("path:" + path);    // 이미지 저장경로 console에 확인
            //해당 디렉토리 확인
            if (!folder.exists()) {
                try {
                    folder.mkdirs(); // 폴더 생성
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }
            out = new FileOutputStream(ckUploadPath);
            out.write(bytes);
            out.flush(); // outputStram에 저장된 데이터를 전송하고 초기화

            String callback = request.getParameter("CKEditorFuncNum");
            printWriter = response.getWriter();
            String fileUrl = "/postforum/ckImgSubmit?uid=" + uid + "&fileName=" + fileName; // 작성화면

            // 업로드시 메시지 출력
            printWriter.println("{\"filename\" : \"" + fileName + "\", \"uploaded\" : 1, \"url\":\"" + fileUrl + "\"}");
            printWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (printWriter != null) {
                    printWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return;
    }

    // 서버로 전송된 이미지 뿌려주기
    @RequestMapping(value="/ckImgSubmit")
    public void ckSubmit(@RequestParam(value="uid") String uid
            , @RequestParam(value="fileName") String fileName
            , HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //서버에 저장된 이미지 경로
        String path = "/resources/uploads/";    // 이미지 경로 설정(폴더 자동 생성)
        System.out.println("path:" + path);
        String sDirPath = path + uid + "_" + fileName;

        sDirPath = uploadPath + File.separator + "uploads" + File.separator + uid + "_" + fileName;


        File imgFile = new File(sDirPath);

        //사진 이미지 찾지 못하는 경우 예외처리로 빈 이미지 파일을 설정한다.
        if (imgFile.isFile()) {
            byte[] buf = new byte[1024];
            int readByte = 0;
            int length = 0;
            byte[] imgBuf = null;

            FileInputStream fileInputStream = null;
            ByteArrayOutputStream outputStream = null;
            ServletOutputStream out = null;

            try {
                fileInputStream = new FileInputStream(imgFile);
                outputStream = new ByteArrayOutputStream();
                out = response.getOutputStream();

                while ((readByte = fileInputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, readByte);
                }

                imgBuf = outputStream.toByteArray();
                length = imgBuf.length;
                out.write(imgBuf, 0, length);
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                outputStream.close();
                fileInputStream.close();
                out.close();
            }
        }
    }

    // 글 목록 조회
    @GetMapping("/list")
    public String list(Model model){
        // 게시판 idx
        // [*****] 쿼리 스트링으로 가져오도록 수정
        // [*****] public String List(@RequestParam("boardIdx") int boardIdx)
        int boardIdx = 3;

        ArrayList<PostForumDTO> postForumDTOList = postForumServiceImpl.list(boardIdx);
        model.addAttribute("postForumDTOList", postForumDTOList);
        return "postForum/list";
    }

    // 글 상세 조회
    @GetMapping("/detail")
    public String detailForm(@RequestParam("postIdx") int postIdx, Model model){
        // 글 정보 불러오기
        PostForumDTO postForumDTO = postForumServiceImpl.detailForm(postIdx);
        model.addAttribute("postForumDTO", postForumDTO);
        return "postForum/detail";
    }

    // 글 수정하기 : 화면 출력
    @GetMapping("/update")
    public String updateForm(@RequestParam("postIdx") int postIdx, Model model){
        PostForumDTO postForumDTO = postForumServiceImpl.findByPostIdx(postIdx);
        model.addAttribute("postForumDTO", postForumDTO);
        return "postForum/update";
    }

    // 글 수정하기
    @PostMapping("/update")
    public String update(@Valid @ModelAttribute("postForumDTO") PostForumDTO postForumDTO, BindingResult errors){
        log.info("update" + errors);
        log.info("update" + postForumDTO);

        // 객체 바인딩에 유효성 오류가 존재한다면, 작성 페이지로 돌아가서 오류 메세지를 출력한다.
        if(errors.hasErrors()){
            return "redirect:/postForum/update?postIdx=" + postForumDTO.getPostIdx();
        }

        // 글 수정하기
        boolean result = postForumServiceImpl.update(postForumDTO);

        if(result) { // 수정 성공
            return "redirect:/postforum/detail?postIdx=" + postForumDTO.getPostIdx();
        }else{ // 수정 실패
            return "redirect:/postforum/list";
        }
    }

    // 글 삭제하기
    @GetMapping("/delete")
    public String delete(@RequestParam("postIdx") int postIdx){
        postForumServiceImpl.delete(postIdx);

        // 삭제 후, 목록 조회 페이지로 돌아간다.
        return "redirect:/postforum/list";
    }
}
