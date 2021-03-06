package com.kh.wheregarden.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kh.wheregarden.domain.board.dao.BoardDAO;
import com.kh.wheregarden.domain.board.dto.BoardDTO;
import com.kh.wheregarden.domain.board.dto.SearchDTO;
import com.kh.wheregarden.domain.board.svc.BoardSVC;
import com.kh.wheregarden.domain.comments.dao.CommentsDAO;
import com.kh.wheregarden.domain.comments.dto.CommentsDTO;
import com.kh.wheregarden.domain.comments.svc.CommentsSVC;
import com.kh.wheregarden.domain.common.dto.MetaOfUploadFile;
import com.kh.wheregarden.domain.common.dto.UpLoadFileDTO;
import com.kh.wheregarden.domain.common.file.FileStore;
import com.kh.wheregarden.domain.common.paging.FindCriteria;
import com.kh.wheregarden.web.form.board.ModifyForm;
import com.kh.wheregarden.web.form.board.ReplyForm;
import com.kh.wheregarden.web.form.board.WriteForm;
import com.kh.wheregarden.web.form.comment.CommentWriteForm;
import com.kh.wheregarden.web.form.login.LoginMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/board")
public class BoardController {
	
	private final BoardSVC boardSVC;
	private final BoardDAO boardDAO;
	private final FileStore fileStore;
//	@Autowired @Qualifier("pc10")
//	private PageCriteria pc;
	@Autowired
	@Qualifier("fc10")
	private FindCriteria fc;
	
	private final CommentsSVC commentsSVC;
	
	//????????? ??????
	@GetMapping({"/boardList",
							 "/boardList/{reqPage}",
							 "/boardList/{reqPage}/{searchType}/{keyword}"})
	public String list(
			@RequestParam(required = false) String category,
			@PathVariable(required = false) Integer reqPage,
			@PathVariable(required = false) String searchType,
			@PathVariable(required = false) String keyword,	
			Model model
			) {
		
		List<BoardDTO> list = null;
		
		//?????????????????? ????????? 1????????????
		if(reqPage == null) reqPage = 1;
		//???????????? ????????? ???????????????
		fc.getRc().setReqPage(reqPage);	
		
		//????????? ??????
		if((searchType == null || searchType.equals(""))
				&& (keyword == null || keyword.equals(""))) {
			
			//????????? ?????????????????? ??????
			fc.setTotalRec(boardSVC.totoalRecordCount(category));
			
			list = boardSVC.list(
					category,
					fc.getRc().getStartRec(),
					fc.getRc().getEndRec());
			log.info("boardList:{}",list);
		}else {
			//????????? ??????????????????
			fc.setTotalRec(boardSVC.totoalRecordCount(category,searchType,keyword));
			
			list = boardSVC.list(
					new SearchDTO(
							category, 
							fc.getRc().getStartRec(), fc.getRc().getEndRec(), 
							searchType, keyword)
			);						
		}
		
		fc.setSearchType(searchType);
		fc.setKeyword(keyword);
				
		model.addAttribute("boardList", list);
		model.addAttribute("fc", fc);
		model.addAttribute("category",category);
		
		return "board/boardList";
	}
	
	//????????? ?????? ??????
	@GetMapping("/{bnum}")
	public String boardDetail(
			@PathVariable Long bnum,
			Model model,
			HttpServletRequest request) {
		
		model.addAttribute("boardDetail", boardSVC.boardDetail(bnum));
		
		//?????? ?????? ??????
		CommentWriteForm newCommentWriteForm = new CommentWriteForm();
		model.addAttribute("CommentWriteForm", newCommentWriteForm);
		
		//??????????????????
		List<CommentsDTO> commentsDTOList = commentsSVC.showComment(bnum);
		
		//????????? ?????? null ??????
		if(commentsDTOList.isEmpty()) {
			model.addAttribute("commentsDTOList", null);
		}
		else {
			model.addAttribute("commentsDTOList", commentsDTOList);
		}
		
		//??????
    HttpSession session = request.getSession(false);
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
		
		return "board/boardDetail";
	}
	
	//????????? ?????? ??????
	@GetMapping("/boardWrite")
	public String boardWrite(
			@RequestParam String category,
			Model model,
			HttpServletRequest request) {
		
		WriteForm writeForm = new WriteForm();
		
		HttpSession session = request.getSession(false);
		if(session != null && session.getAttribute("loginMember") != null) {
			LoginMember loginMember = (LoginMember)session.getAttribute("loginMember");
			
			writeForm.setBmid(loginMember.getId());
			writeForm.setBnickname(loginMember.getNickname());
			writeForm.setBcategory(category);
			}
		
		model.addAttribute("writeForm", writeForm);
		model.addAttribute("category", category);
		
		return "board/boardWrite";
	}


	//????????? ?????? ??????
	@PostMapping("/boardWrite")
	public String write(
			//@RequestParam String cate,
			@Valid @ModelAttribute WriteForm writeForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) throws IllegalStateException, IOException {
	
		//?????? ??? ????????? ???????????????
		if(bindingResult.hasErrors()) {
			bindingResult.reject("error.write", "?????? ??? ????????? ????????? ???????????????.");
			return "board/boardWrite";
		}
		
		log.info("writeForm:{}",writeForm);
		BoardDTO boardDTO = new BoardDTO();
		BeanUtils.copyProperties(writeForm, boardDTO);
		
		log.info("getFiles:{}",writeForm.getFiles());
		//???????????? ?????????????????? ????????? ???????????? ??????
		List<MetaOfUploadFile> storedFiles = fileStore.storeFiles(writeForm.getFiles());
		log.info("storedFiles:{}",storedFiles);
		//UploadFileDTO ??????
		boardDTO.setFiles(convert(storedFiles));
		
		log.info("?????? ??? ?????????:{}",boardDTO);					// ????????? ??? ??? rid??? ????????? ??????
		
		Long bnum = boardSVC.boardWrite(boardDTO);
		
		redirectAttributes.addAttribute("bnum", bnum);
		return "redirect:/board/{bnum}";
	}
	
	private UpLoadFileDTO convert(MetaOfUploadFile attatchFile) {
		UpLoadFileDTO uploadFileDTO = new UpLoadFileDTO();
		BeanUtils.copyProperties(attatchFile, uploadFileDTO);
		return uploadFileDTO;
	}
	
	private List<UpLoadFileDTO> convert(List<MetaOfUploadFile> uploadFiles) {
		List<UpLoadFileDTO> list = new ArrayList<>();
	
		for(MetaOfUploadFile file : uploadFiles) {
			UpLoadFileDTO uploadFIleDTO = convert(file);
			list.add( uploadFIleDTO );
		}		
		return list;
	}

	//????????? ?????? ??????
	@GetMapping("/boardModify/{bnum}")
	public String editForm(
			@PathVariable Long bnum,
			Model model) {
			
		model.addAttribute("modifyForm", boardSVC.boardDetail(bnum)) ;
		return "board/boardModify";
	}
	
	//????????? ?????? ??????
	@PatchMapping("/boardModify/{bnum}")
	public String edit(
			@PathVariable Long bnum,
			@Valid @ModelAttribute ModifyForm modifyForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) throws IllegalStateException, IOException {
		
		//?????? ??? ????????? ???????????????
		if(bindingResult.hasErrors()) {
			bindingResult.reject("error.write", "?????? ??? ????????? ????????? ???????????????.");
			return "board/boardModify";
		}
		
		BoardDTO boardDTO = new BoardDTO();
		
		//???????????? ?????????????????? ????????? ???????????? ??????
		List<MetaOfUploadFile> storedFiles = fileStore.storeFiles(modifyForm.getFiles());
		//UploadFileDTO ??????
		boardDTO.setFiles(convert(storedFiles));		
		BeanUtils.copyProperties(modifyForm, boardDTO);
		
		Long modifyedBnum = boardSVC.boardModify(bnum, boardDTO);
		redirectAttributes.addAttribute("bnum", modifyedBnum);
		
		return "redirect:/board/{bnum}";
	}
	
	//?????? ?????? ??????
	@GetMapping("/replyQnA/{bnum}")
	public String replyForm(
			@PathVariable Long bnum,
			Model model,
			HttpServletRequest request) {		
		
		ReplyForm replyForm = new ReplyForm();
		
		//???????????? ?????? id,email,nickname????????????
		HttpSession session = request.getSession(false);
		if(session != null && session.getAttribute("loginMember") != null) {
			LoginMember loginMember = 
					(LoginMember)session.getAttribute("loginMember");
			
			replyForm.setBmid(loginMember.getId());
			replyForm.setBnickname(loginMember.getNickname());
		}
		
		//???????????? ?????????, ????????????, ?????? ????????????
		BoardDTO pBoardDTO = boardSVC.boardDetail(bnum);
		replyForm.setBpnum(pBoardDTO.getBnum());
		replyForm.setBcategory(pBoardDTO.getBcategory());
		replyForm.setBtitle("?????? : " + pBoardDTO.getBtitle());
		
		model.addAttribute("pBoardDTO", pBoardDTO);
		model.addAttribute("replyForm", replyForm);
		
		return "board/replyWrite";
	}
	
	//?????? ?????? ??????
	@PostMapping("/replyQnA/{bnum}")
	public String reply(
			@PathVariable("bnum") Long bpnum,  //?????????
			@Valid @ModelAttribute ReplyForm replyForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) throws IllegalStateException, IOException {
	
		//?????? ??? ????????? ???????????????
		if(bindingResult.hasErrors()) {
			bindingResult.reject("error.write", "?????? ??? ????????? ????????? ???????????????.");
			return "board/replyWrite";
		}
		
		BoardDTO replyboardDTO = new BoardDTO();
		BeanUtils.copyProperties(replyForm, replyboardDTO);
		
		//???????????? bnum, bgroup, bstep, bindent
		BoardDTO pboardDTO = boardSVC.boardDetail(bpnum);
		replyboardDTO.setBpnum(pboardDTO.getBnum());			//??????????????? ???????????? ????????? ?????????????????? ??????
		replyboardDTO.setBgroup(pboardDTO.getBgroup());		//??????????????? ???????????? ????????? ????????? ??????
		replyboardDTO.setBstep(pboardDTO.getBstep());			//
		replyboardDTO.setBindent(pboardDTO.getBindent());
		
		//???????????? ?????????????????? ????????? ???????????? ??????
		List<MetaOfUploadFile> storedFiles = fileStore.storeFiles(replyForm.getFiles());
		//UploadFileDTO ??????
		replyboardDTO.setFiles(convert(storedFiles));
		
		Long rbnum = boardSVC.replyWrite(replyboardDTO);
		
		redirectAttributes.addAttribute("bnum", rbnum);
		return "redirect:/board/{bnum}";
	}
	
	//?????? ??? ??? ??????
	@GetMapping({"/myBoardList",
							 "/myBoardList/{reqPage}",
							 "/myBoardList/{reqPage}/{searchType}/{keyword}"})
	public String myBoardList(
			@RequestParam(required = false) String category,
			@PathVariable(required = false) Integer reqPage,
			@PathVariable(required = false) String searchType,
			@PathVariable(required = false) String keyword,
			HttpServletRequest request,
			Model model
			) {
		
		//?????? ????????????
		HttpSession session = request.getSession(false);
		//if(session == null) return "redirect:/login";
		
		LoginMember loginMember = (LoginMember)session.getAttribute("loginMember");
		log.info("????????? ??? ?????????:{}",loginMember.getId());
		
		List<BoardDTO> list = null;
		
		//?????????????????? ????????? 1????????????
		if(reqPage == null) reqPage = 1;
		//???????????? ????????? ???????????????
		fc.getRc().setReqPage(reqPage);	
		
		//????????? ??????
		if((searchType == null || searchType.equals(""))
				&& (keyword == null || keyword.equals(""))) {
			
			//????????? ?????????????????? ??????
			fc.setTotalRec(boardSVC.myTotalRecordCount(category, loginMember.getId()));
			log.info("?????? ???????????? :{}",boardSVC.myTotalRecordCount(category, loginMember.getId()));
			list = boardSVC.myList(
					category,
					loginMember.getId(),
					fc.getRc().getStartRec(),
					fc.getRc().getEndRec());
			log.info("????????? ?????? myBoardList:{}",list);
		}
		else {
			//????????? ??????????????????
			fc.setTotalRec(boardSVC.myTotalRecordCount(category, loginMember.getId(), searchType, keyword));
			
			list = boardSVC.myList(
					new SearchDTO(
							category, 
							fc.getRc().getStartRec(), fc.getRc().getEndRec(), 
							searchType, keyword)
					, loginMember.getId()
			);
			log.info("????????? ?????? myBoardList:{}",list);
		}
		
		fc.setSearchType(searchType);
		fc.setKeyword(keyword);
				
		model.addAttribute("boardList", list);
		model.addAttribute("fc", fc);
		model.addAttribute("category",category);
		
		return "board/myBoardList";
	}
	
	
	//?????? ??????
	@PostMapping("/comment/{cbnum}")
	public String commentWrite(
			CommentWriteForm commentWriteForm,
			@PathVariable Long cbnum,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		
		CommentsDTO newCommentsDTO = new CommentsDTO();
		BeanUtils.copyProperties(commentWriteForm, newCommentsDTO);
		
		newCommentsDTO.setCbnum(cbnum);
		
		HttpSession session = request.getSession(false);
		if(session != null && session.getAttribute("loginMember") != null) {
			LoginMember loginMember = (LoginMember)session.getAttribute("loginMember");
			
			newCommentsDTO.setCid(loginMember.getId());
			newCommentsDTO.setCnickname(loginMember.getNickname());
			}
		
		log.info("????????? ?????? DTO : {}", newCommentsDTO);
		commentsSVC.writeComment(newCommentsDTO);
		
		redirectAttributes.addAttribute("bnum", cbnum);
		return "redirect:/board/{bnum}";
	}
	
	//?????? ??????
	@GetMapping("/comment/del/{cnum}")
	public String delComment(
			@PathVariable Long cnum,
			RedirectAttributes redirectAttributes){
		
		CommentsDTO foundCommentsDTO = commentsSVC.findParentComment(cnum);
		Long cbnum = foundCommentsDTO.getCbnum();
		
		commentsSVC.delComment(cnum);
		
		redirectAttributes.addAttribute("bnum", cbnum);
		return "redirect:/board/{bnum}";
	}
	
}