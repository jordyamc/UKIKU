
(function(win, doc) {
	'use strict';
	
	var WBMusicPlayer = function(elem, options) {
		this._items = [];
		this._elem = elem;
		var thisSelf = this,
			opts = (options || {}),
			initLoad = false,
			base = doc.createElement('div'),
			ctrl = doc.createElement('div'),
			ctrlBtns = doc.createElement('div'),
			audio = doc.createElement('audio'),
			progressBar = doc.createElement('div'),
			progressVal = doc.createElement('div');
		progressVal.className = 'wb-music-player-pval';
		progressVal.style.width = '0%';
		progressBar.className = 'wb-music-player-pbar';
		progressBar.appendChild(progressVal);
		this._btnPrev = doc.createElement('a');
		this._btnNext = doc.createElement('a');
		this._btnPlay = doc.createElement('a');
		this._btnPrev.className = 'wb-music-player-prev';
		this._btnPrev.innerHTML = '<span class="glyphicon glyphicon-step-backward"></span>';
		this._btnPrev.setAttribute('href', 'javascript:void(0)');
		this._btnPrev.addEventListener('click', function() {
			if (thisSelf._itemIndex > 0) {
				thisSelf._itemIndex--;
				thisSelf._updateState();
				var wasPlaying = thisSelf._isPlaing;
				if (wasPlaying) {
					thisSelf._audio.pause();
				}
				thisSelf._audio.load(thisSelf._items[thisSelf._itemIndex].url);
				if (wasPlaying) {
					thisSelf._audio.play();
				}
			}
		});
		this._btnNext.className = 'wb-music-player-next';
		this._btnNext.innerHTML = '<span class="glyphicon glyphicon-step-forward"></span>';
		this._btnNext.setAttribute('href', 'javascript:void(0)');
		this._btnNext.addEventListener('click', function() {
			if (thisSelf._itemIndex < (thisSelf._items.length - 1)) {
				thisSelf._itemIndex++;
				thisSelf._updateState();
				var wasPlaying = thisSelf._isPlaing;
				if (wasPlaying) {
					thisSelf._audio.pause();
				}
				thisSelf._audio.load(thisSelf._items[thisSelf._itemIndex].url);
				if (wasPlaying) {
					thisSelf._audio.play();
				}
			}
		});
		this._btnPlay.className = 'wb-music-player-play';
		this._btnPlay.innerHTML = '<span class="glyphicon glyphicon-play"></span>';
		this._btnPlay.setAttribute('href', 'javascript:void(0)');
		ctrlBtns.className = 'wb-music-player-btns';
		ctrlBtns.appendChild(this._btnPrev);
		ctrlBtns.appendChild(this._btnPlay);
		ctrlBtns.appendChild(this._btnNext);
		audio.setAttribute('preload', '');
		this._plist = doc.createElement('div');
		ctrl.className = 'wb-music-player-ctrl';
		ctrl.appendChild(audio);
		ctrl.appendChild(ctrlBtns);
		ctrl.appendChild(progressBar);
		this._plist.className = 'wb-music-player-plist';
		base.className = 'wb-music-player';
		base.appendChild(ctrl);
		base.appendChild(this._plist);
		this._elem.appendChild(base);
		this._updateState();
		if (win.audiojs && !opts.noInit) {
			win.audiojs.events.ready(function() {
				thisSelf._audio = win.audiojs.create(audio, {
					autoplay: (opts.autoplay ? true : false),
					createPlayer: {
						markup: '',
						playPauseClass: 'wb-music-player-play',
						scrubberClass: 'wb-music-player-pbar'
					},
					css: '',
					init: function() {
						if (!initLoad && thisSelf._itemIndex >= 0 && thisSelf._itemIndex < thisSelf._items.length) {
							initLoad = true;
							thisSelf._audio.setVolume(0.5);
							thisSelf._audio.load(thisSelf._items[thisSelf._itemIndex].url);
						}
					},
					play: function() {
						thisSelf._isPlaing = true;
						thisSelf._updateState();
					},
					pause: function() {
						thisSelf._isPlaing = false;
						thisSelf._updateState();
					},
					trackEnded: function() {
						if (!opts.autoplay) return;
						if (thisSelf._items.length > 1) {
							thisSelf._itemIndex++;
							if (thisSelf._itemIndex >= thisSelf._items.length) thisSelf._itemIndex = 0;
						}
						if (thisSelf._itemIndex >= 0 && thisSelf._itemIndex < thisSelf._items.length) {
							thisSelf._audio.load(thisSelf._items[thisSelf._itemIndex].url);
							thisSelf._audio.play();
						}
						thisSelf._updateState();
					},
					updatePlayhead: function(percent) {
						progressVal.style.width = Math.round(100 * percent) + '%';
					},
					loadStarted: function() {},
					loadProgress: function(percent) {},
					loadError: function(e) {},
					flashError: function() {}
				});
			});
		}
	};
	WBMusicPlayer.prototype._elem = null;
	WBMusicPlayer.prototype._plist = null;
	WBMusicPlayer.prototype._btnPrev = null;
	WBMusicPlayer.prototype._btnNext = null;
	WBMusicPlayer.prototype._btnPlay = null;
	WBMusicPlayer.prototype._items = null;
	WBMusicPlayer.prototype._itemIndex = -1;
	WBMusicPlayer.prototype._isPlaing = false;
	WBMusicPlayer.prototype._updateState = function() {
		var i, cls,
			prevCls = this._btnPrev.className.replace(' disabled', ''),
			nextCls = this._btnNext.className.replace(' disabled', ''),
			playCls = this._btnPlay.className.replace(' disabled', ''),
			iconCls = this._btnPlay.firstChild.className.replace(' glyphicon-play', '').replace(' glyphicon-pause', '');
		if (this._itemIndex < 0) {
			playCls += ' disabled';
		}
		if (this._items.length <= 1) {
			prevCls += ' disabled';
			nextCls += ' disabled';
		} else if (this._itemIndex === 0) {
			prevCls += ' disabled';
		} else if (this._itemIndex >= (this._items.length - 1)) {
			nextCls += ' disabled';
		}
		iconCls += this._isPlaing ? ' glyphicon-pause' : ' glyphicon-play';
		this._btnPrev.className = prevCls;
		this._btnNext.className = nextCls;
		this._btnPlay.className = playCls;
		this._btnPlay.firstChild.className = iconCls;
		for (i = 0; i < this._items.length; i++) {
			cls = this._items[i].elem.className.replace(' active', '');
			if (this._itemIndex === i) cls += ' active';
			this._items[i].elem.className = cls;
		}
	};
	WBMusicPlayer.prototype.removeAllItems = function() {
		this._itemIndex = -1;
		for (var i = 0; i < this._items.length; i++) {
			this._plist.removeChild(this._items[i].elem);
			delete this._items[i].elem;
		}
		this._items.splice(0, this._items.length);
		this._updateState();
	};
	WBMusicPlayer.prototype.addAllItems = function(items) {
		var i, src, name, m, elem;
		for (i = 0; i < items.length; i++) {
			if (typeof items[i] !== 'string') {
				if (!('src' in items[i]) || !items[i].src) continue;
				if (!('name' in items[i])) items[i].name = items[i].src;
				src = items[i].src;
				name = items[i].name;
			} else {
				src = items[i];
				name = items[i];
			}
			name = (m = ('' + name).match(/^.*\/([^\/\.]+)\.[^\.]+$/i)) ? m[1] : name;
			elem = doc.createElement('div');
			elem.innerHTML = name;
			if (this._itemIndex < 0) this._itemIndex = this._items.length;
			this._items.push({url: src, name: name, elem: elem});
			this._plist.appendChild(elem);
		}
		this._updateState();
	};
	
	win.WBMusicPlayer = {
		create: function(elem, options) {
			return new WBMusicPlayer(elem, options);
		}
	};
})(window, document);
