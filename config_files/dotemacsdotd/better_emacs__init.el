(require 'package)
(add-to-list 'package-archives
	     '("marmalade" . "http://marmalade-repo.org/packages/"))
(package-initialize)

;; Disable loading of “default.el” at startup,
(setq inhibit-default-init t)

;;https://github.com/glasserc/ethan-wspace
(add-to-list 'load-path (expand-file-name "~/.emacs.d/upstream/ethan-wspace/lisp"))
(require 'ethan-wspace)
(global-ethan-wspace-mode 1)

(defvar my-packages '(better-defaults
		      clojure-mode
		      cider))
(dolist (p my-packages)
  (when (not (package-installed-p p))
    (package-install p)))
;;;;;;;;;;;
(require 'xt-mouse)
(xterm-mouse-mode)
(require 'mouse)
(xterm-mouse-mode t)
(defun track-mouse (e))

(setq mouse-wheel-follow-mouse 't)

(defvar alternating-scroll-down-next t)
(defvar alternating-scroll-up-next t)

(defun alternating-scroll-down-line ()
  (interactive "@")
  (when alternating-scroll-down-next
                                        ;      (run-hook-with-args 'window-scroll-functions )
    (scroll-down-line))
  (setq alternating-scroll-down-next (not alternating-scroll-down-next)))

(defun alternating-scroll-up-line ()
  (interactive "@")
  (when alternating-scroll-up-next
                                        ;      (run-hook-with-args 'window-scroll-functions)
    (scroll-up-line))
  (setq alternating-scroll-up-next (not alternating-scroll-up-next)))

(global-set-key (kbd "<mouse-4>") 'alternating-scroll-down-line)
(global-set-key (kbd "<mouse-5>") 'alternating-scroll-up-line)
;;;;;;;;;;;;;;;;;;

(setq-default mode-line-buffer-identification
              (let ((orig  (car mode-line-buffer-identification)))
                `(:eval (cons (concat ,orig (abbreviate-file-name default-directory))
                                                            (cdr mode-line-buffer-identification)))))
;;;;;

(add-hook 'cider-mode-hook 'cider-turn-on-eldoc-mode)
(setq nrepl-hide-special-buffers t)

(setq save-interprogram-paste-before-kill nil)
(put 'downcase-region 'disabled nil)

(setq x-select-enable-clipboard t
      x-select-enable-primary t)

(global-linum-mode t)

(desktop-save-mode 1)

(xclip-mode 1)

(setq mode-require-final-newline nil)

;; Jumps backwards around the global mark ring (opposite of "C-u C-SPC")
(defun unpop-to-mark-command ()
  "Unpop off mark ring. Does nothing if mark ring is empty."
  (interactive)
  (when mark-ring
    (setq mark-ring (cons (copy-marker (mark-marker)) mark-ring))
    (set-marker (mark-marker) (car (last mark-ring)) (current-buffer))
    (when (null (mark t)) (ding))
    (setq mark-ring (nbutlast mark-ring))
    (goto-char (marker-position (car (last mark-ring))))))

(global-set-key (kbd "M-g M-SPC") 'unpop-to-mark-command)
