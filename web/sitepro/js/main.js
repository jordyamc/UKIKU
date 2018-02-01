
function wb_form_validateForm(formId, values, errors) {
	var form = $("input[name='wb_form_id'][value='" + formId + "']").parent();
	if (!form || form.length === 0 || !errors) return;
	
	form.find("input[name],textarea[name]").css({backgroundColor: ""});
	
	if (errors.required) {
		for (var i = 0; i < errors.required.length; i++) {
			var name = errors.required[i];
			var elem = form.find("input[name='" + name + "'],textarea[name='" + name + "'],select[name='" + name + "']");
			elem.css({backgroundColor: "#ff8c8c"});
		}
	}
	
	if (Object.keys(errors).length) {
		for (var k in values) {
			var elem = form.find("input[name='" + k + "'],textarea[name='" + k + "'],select[name='" + k + "']");
			elem.val(values[k]);
		}
	}
}

$(function() {
	var comboBoxes = $('.wb-combobox-controll');
	if (comboBoxes.length) {
		comboBoxes.each(function() {
			var thisCombo = $(this);
			var clickFunc = function() {
				var w = thisCombo.find('input').outerWidth();
				var mw = (menu = thisCombo.find('.dropdown-menu')).width();
				var ew = thisCombo.parent().outerWidth();
				if (mw < ew) menu.width(ew);
				menu.css({ marginLeft: (-w) + 'px' });
				thisCombo.find('.btn-group').toggleClass('open');
			};
			$(this).find('input').bind('click', clickFunc);
			$(this).find('.dropdown-toggle').bind('click', clickFunc);
		});
		
		$(document).bind('click', function(e) {
			var t = $(e.target);
			if (!t.is('.wb-combobox-controll')) {
				t = t.parents('.wb-combobox-controll');
				$.each($('.wb-combobox-controll'), function() {
					if (t.get(0) !== $(this).get(0)) {
						$(this).find('.btn-group').removeClass('open');
					}
				});
			}
		});
	}
	if (currLang) {
		$('.lang-selector').each(function() {
			var thisElem = $(this);
			var type = thisElem.attr('data-type');
			if (type === 'flags') {
				thisElem.find('a[data-lang="' + currLang + '"]').addClass('active');
			} else if (type === 'select') {
				var actLi = thisElem.find('li[data-lang="' + currLang + '"]');
				actLi.addClass('active');
				thisElem.find('input').val(actLi.find('a').html());
			}
		});
	}
	$('.btn-group.dropdown').each(function() {
		var ddh = $(this).height();
		var ddm = $(this).children('.dropdown-menu');
		ddm.addClass('open');
		var ddmh = ddm.height();
		ddm.removeClass('open');
		var ddt = $(this).offset().top;
		var dh = $(document).height();
		if (ddt + ddh + ddmh + 2 >= dh) {
			$(this).removeClass('dropdown').addClass('dropup');
		}
	});

	var closeMenus = function(ignoreMenu) {
		$('.wb-menu').each(function() {
			if( this == ignoreMenu )
				return;
			$(this).find('.over').removeClass('over over-out');
		});
	};

	$('body').on('touchstart', function(e) {
		var ignoreMenu = $(e.target).closest('.wb-menu');
		ignoreMenu = ignoreMenu.length ? ignoreMenu.get(0) : null;
		closeMenus(ignoreMenu);
	});

	$('.wb-menu').each(function() {
		var $menuContainer = $(this);
		var $menu = $menuContainer.children('ul');
		var ignoreHover = null;
		var isLanding = $menu.is('.menu-landing');
		var selectMenuItem = function($elem) {
			$elem.addClass('over');
			var $parent = $elem;
			while( $parent.length > 0 && $parent.is('li') ) {
				$parent.removeClass('over-out');
				$parent = $parent.parent().parent();
			}
			if( $menuContainer.is('.collapse-expanded') ) {
				$menu.find('.open-left').removeClass('open-left');
			}
			else {
				var $submenu = $elem.children('ul');
				if( $submenu.length ) {
					$parent = $elem.parent();
					if( $menu.is('.vmenu') && $parent.is('.open-left') ) {
						$submenu.addClass('open-left');
					}
					else {
						$submenu.removeClass('open-left');
						var ww = $(window).width();
						var w = $submenu.outerWidth(true);
						if( $submenu.offset().left + w >= ww )
							$submenu.addClass('open-left');
					}
					if( $submenu.offset().left < 0 )
						$submenu.removeClass('open-left');
				}
			}
		};
		var closeMenu = function() {
			$menu.find('li.over').addClass('over-out');
			setTimeout(function() {
				$menu.find('li.over-out').removeClass('over over-out');
			}, 10);
		};
		$menu
			.on('mouseover', 'li', function(e) {
				if( ignoreHover )
					return;
				selectMenuItem($(this));
			})
			.on('mouseout', 'li', function(e) {
				if( ignoreHover )
					return;
				closeMenu();
			})
			.on('touchstart', 'a', function(e) {
				var $elem = $(this).parent();
				var isOver = $elem.is('.over') || ($menuContainer.is('.collapse-expanded') && $elem.is('.active'));

				if( ignoreHover )
					clearTimeout(ignoreHover);
				ignoreHover = setTimeout(function() {ignoreHover = null;}, 2000);

				closeMenus($menuContainer.get(0));
				closeMenu();
				selectMenuItem($elem);

				if( isOver || $elem.children('ul').length == 0 ) {
					if( isLanding )
						e.stopImmediatePropagation();
				}
				else {
					e.stopImmediatePropagation();
					e.preventDefault();
				}
			})
		;
	});

	$('.wb-menu-mobile').each(function() {
		var isOpen = false;
		var elem = $(this);
		var btn = elem.children('.btn-collapser').eq(0);
		var isLanding = (elem.children('.menu-landing').length > 0 || elem.parents('.wb_header_fixed').length > 0);

		var onResize = function() {
			var ul = elem.children('ul');
			ul.css('max-height', ($(window).scrollTop() - ul.offset().top + $(window).height() - 20) + 'px');
		};
		
		var updateMenuPosition = function() {
			elem.children('ul').css({top: (btn.offset().top + btn.outerHeight() - $(window).scrollTop()) + 'px'});
		};
		
		btn.on('click', function() {
			if (elem.hasClass('collapse-expanded')) {
				isOpen = false;
				elem.removeClass('collapse-expanded');
			} else {
				isOpen = true;
				elem.addClass('collapse-expanded');
				updateMenuPosition();
				if (isLanding) onResize();
			}
		});
		
		$(window).scroll(function() { updateMenuPosition(); });
		
		if( isLanding ) {
			$(window).on('resize', onResize);
			elem.find('li').on('click', function() {
				isOpen = false;
				elem.removeClass('collapse-expanded');
			});
		}
		/*
		elem.find('ul').each(function() {
			var ul = $(this);
			if (ul.parent('li').length > 0) {
				ul.parent('li').eq(0).children('a').on('click', function() {
					if (!isOpen) return true;
					if (ul.css('display') !== 'block') ul.css({display: 'block'}); else ul.css({display: ''});
					return false;
				});
			}
		});
		*/
	});
	
	if ($('.menu-landing').length) {
		var scrolled = false;
		var activateMenuItem = function(item) {
//			item.closest('.wb-menu').find('li.active').removeClass('active');
			while( item.length > 0 && item.is('li') ) {
				item.addClass('active');
				item = item.parent().parent();
			}
		};
		var switchLandingPage = function(alias, ln, scroll) {
			ln = ln || currLang;
			var href = ln ? ln + '/#' + alias : '#' + alias;
			var anchor = $('.wb_page_anchor[name="' + alias + '"]');
			if (anchor.length) {
				if (scroll) {
					anchor.attr('name', '');
					setTimeout(function() {
						anchor.attr('name', alias);
					}, 10);
					scrolled = true;
					$('html, body').animate({ scrollTop: anchor.offset().top + 'px' }, 540, function() {
						scrolled = false;
					});
				}
			}
			var item = $('.menu-landing li a[href="' + href + '"]').parent();
			$('.menu-landing li').removeClass('active');
			if (item.length) {
				activateMenuItem(item);
			}
		};
		$('.menu-landing li a').on('click', function() {
			var href = $(this).attr('href'), parts = href.split('#'),
				ln = parts[0] ? parts[0].replace(/\/$/, '') : null,
				alias = parts[1];
				
			if (/^(?:http|https):\/\//.test(href)) return true;
			switchLandingPage(alias, ln, true);
		});
		$(window).on('hashchange', function() {
			var link = $('.menu-landing li a[href="' + location.hash + '"]');
			if (link.length) {
				var item = link.parent();
				activateMenuItem(item);
			}
		});
		$(window).bind('scroll', function() {
			if (scrolled) return false;
			var anchors = $('.wb_page_anchor');
			$(anchors.get().reverse()).each(function() {
				if ($(this).offset().top <= $(window).scrollTop() + $('#wb_header').height()) {
					var alias = $(this).attr('name');
					switchLandingPage(alias);
					return false;
				}
			});
		});
		$(window).trigger('hashchange');
		
		window.wbIsLanding = true;
	}
	
	$(document).on('mousedown', '.ecwid a', function() {
		var href = $(this).attr('href');
		if (href && href.indexOf('#!') === 0) {
			var url = decodeURIComponent(location.pathname) + href;
			$(this).attr('href', url);
		}
	});

	var applyAutoHeight = function(selector, getElementsCallback, getShapesCallback) {
		$(selector).each(function() {
			var currentTop = null;
			var expectedShapes = null;
			var maxHeight = {};
			var forcedHeight = {};
			var elemCount = {};
			var $elements = getElementsCallback($(this));
			var hasErrors = false;
			$elements.each(function() {
				var i;
				var $elem = $(this);
				var shapes = $elem.data('shapes');
				if( !shapes ) {
					var $shapes = getShapesCallback($elem);
					if( $shapes.length == 0 || (expectedShapes !== null && expectedShapes != $shapes.length) ) {
						hasErrors = true;
						return false;
					}
					shapes = [];
					for( i = 0; i < $shapes.length; i++ ) {
						var $shape = $($shapes.get(0));
						shapes[i] = {
							isMap: $shape.is('.wb-map'),
							elem: $shape
						};
					}
					$elem.data('shapes', shapes);
				}
				expectedShapes = shapes.length;
				for( i = expectedShapes - 1; i >= 0; i-- ) {
					if( !shapes[i].isMap )
						shapes[i].elem.css('height', '');
				}
				var top = Math.round($elem.offset().top / 5);
				if( top !== currentTop )
					$elem.css('clear', 'left'); // This is needed to fit more elements on same y position.
				currentTop = Math.round($elem.offset().top / 5);
				$elem.data('aht', currentTop);
				if( !maxHeight.hasOwnProperty(currentTop) ) {
					maxHeight[currentTop] = [];
					for( i = 0; i < expectedShapes; i++ )
						maxHeight[currentTop][i] = 0;
					forcedHeight[currentTop] = false;
					elemCount[currentTop] = 0;
				}
				if( !forcedHeight[currentTop] ) {
					for( i = expectedShapes - 1; i >= 0; i-- ) {
						if( shapes[i].isMap ) {
							maxHeight[currentTop][i] = shapes[i].elem.outerHeight();
							forcedHeight[currentTop] = true; // map element height has top priority
							break;
						}
						else
							maxHeight[currentTop][i] = Math.max(maxHeight[currentTop][i], shapes[i].elem.outerHeight());
					}
				}
				elemCount[currentTop]++;
			});
			if( hasErrors )
				return;
			$elements.each(function() {
				var $elem = $(this);
				var shapes = $elem.data('shapes');
				var aht = $elem.data('aht');
				$elem.css('clear', '');
				if( elemCount[aht] < 2 )
					return;
				for( var i = expectedShapes - 1; i >= 0; i-- )
					if( !shapes[i].isMap )
						shapes[i].elem.css('height', maxHeight[aht][i]);
			});
		});
		if (window.wbIsLanding) {
			$('#wb_main').children('.wb_page').each(function() {
				var bg, pageId = $(this).attr('id'); pageId = pageId.substring(pageId.length - 1);
				if ((bg = $('#wb_page_' + pageId + '_bg')).length) {
					bg.css('height', $(this).outerHeight() + 'px');
				}
			});
		}
		if ($('.wb_header_fixed').length) {
			var headerH = $('.wb_header_fixed').outerHeight(true);
			$('#wb_main').css('padding-top', headerH + 'px');
			$('#wb_header_bg').css('height', headerH + 'px');
			$('#wb_sbg_placeholder').css('height', headerH + 'px');
		}
	};

	var recalcAutoHeightColumns = function() {
		applyAutoHeight('.auto-height', function($cont) {
			return $cont.children('.wb-cs-col');
		}, function($elem) {
			return $elem.children();
		});

		applyAutoHeight('.auto-height2', function($cont) {
			return $cont.children('.wb-cs-col');
		}, function($elem) {
			return $elem.children('.wb-cs-row').children('.wb-cs-col').children('.wb_element_shape');
		});
	};
	
	$(window).on('resize', recalcAutoHeightColumns);
	recalcAutoHeightColumns();
	
	(function() {
		
		var getMinContentHeight = function(elems) {
			var totalH = 0;
			elems.each(function() {
				if ($(this).is(':not(:visible)')) return true;
				var h = $(this).outerHeight();
				var t = parseInt($(this).css('top')); if (isNaN(t)) t = 0;
				totalH = Math.max((h + t), totalH);
			});
			return totalH;
		};

		var applyModeAutoHeight = function() {
			if (!('wbIsAutoLayout' in window)) window.wbIsAutoLayout = ($('.wb-cs-row').length > 0);
			if (window.wbIsAutoLayout) return;

			if (window.wbIsLanding) {
				$('#wb_main > .wb_cont_inner').find('.wb_page').each(function() {
					var bg = null;
					var $this = $(this);
					var pageId = parseInt($this.attr('id').replace(/^page_/, '')); if (isNaN(pageId)) return true;
					var minH = getMinContentHeight($this.children('.wb_element'));
					var outerMinH = getMinContentHeight($('#page_' + pageId + 'e').children('.wb_element'));
					var totalMinH = Math.max(minH, outerMinH);
					var paddingB = parseInt($this.css('padding-bottom')); if (isNaN(paddingB)) paddingB = 0;
					$this.css('height', totalMinH + 'px');
					$("#" + $this.attr('id') + "e").css('height', totalMinH + 'px');
					if ((bg = $('#wb_page_' + pageId + '_bg')).length) {
						bg.css('height', (totalMinH + paddingB) + 'px');
					}
				});
			} else {
				var paddingT = parseInt($('#wb_main').css('padding-top')); if (isNaN(paddingT)) paddingT = 0;
				var paddingB = parseInt($('#wb_main').css('padding-bottom')); if (isNaN(paddingB)) paddingB = 0;
				$('#wb_main').css('height', (getMinContentHeight($('#wb_main').find('.wb_element')) + paddingB + paddingT) + 'px');
			}
			var rootH = 0;
			$('.root').children('.wb_container').each(function() {
				if ($(this).hasClass('wb_header_fixed')) return true;
				rootH += $(this).outerHeight();
			});
			$('body, .root, .wb_sbg').css({ height: (rootH + 'px'), minHeight: (rootH + 'px') });
		};
		
		$(window).on('resize', applyModeAutoHeight);
		applyModeAutoHeight();
		setTimeout(function() {
			applyModeAutoHeight();
		}, 100);
	})();
	
	(function() {
		var header = $('.wb_header_fixed');
		if (header.length) {
			var applyTopToAnchors = function() {
				var header = $('.wb_header_fixed');
				$('.wb_anchor').css('top', (-header.outerHeight()) + 'px');
			};
			$(window).on('resize', applyTopToAnchors);
			applyTopToAnchors();
			setTimeout(function() {
				applyTopToAnchors();
			}, 100);
		}
	})();
	
	(function() {
		if ($('.wb_form_captcha').length) {
			var resizeCaptcha = function() {
				$('.wb_form_captcha').each(function() {
					var form = $(this).parents('.wb_form');
					var cpw = $(this).children(':first').width();
					var tdw = form.width() - form.find('th:first').width();
					var scale = Math.min(tdw / cpw, 1), scaleCss = 'scale(' + scale + ')';
					$(this).css({
						'transform': scaleCss,
						'-o-transform': scaleCss,
						'-ms-transform': scaleCss,
						'-moz-transform': scaleCss,
						'-webkit-transform': scaleCss,
						'max-width': tdw + 'px'
					});
				});
			};
			$(window).on('resize', resizeCaptcha);
			setTimeout(function() {
				resizeCaptcha();
			}, 500);
		}
	})();
	
	var updatePositionFixed = function() {
		$('#wb_bgs_cont > div, body, .wb_sbg').each(function() {
			$(this).css({'background-attachment': 'scroll'});
		});
		$('#wb_header_bg').css('background-image', 'none');
	};
	if (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) {
		updatePositionFixed();
	}
});
