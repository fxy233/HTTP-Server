var ul = document.getElementsByTagName("ul")[0];
var icons = document.getElementsByClassName("icon");

function getStyle(element, attr){
    if(window.getComputedStyle(element,null)){
        return window.getComputedStyle(element,null)[attr];
    }else{
        return element.currentStyle[attr];
    }
}


function changeIconColor(index){  //index值表示第几个内容需要高亮
    
    for(var i = 0; i < 4; i ++){
        icons[i].className = "icon";
    }

    icons[index].className = "icon active";
    

}
var counter = 0;
var firstTime = 0;
var timer = setInterval(function(){

    
    
    if(parseInt(ul.style.left)%500 != 0){
        ul.style.left = parseInt(getStyle(ul,"left")) - 1 +'px';
        if( parseInt(getStyle(ul,"left")) % 500 == -250 ){
            if(counter == 3){
                counter = 0;
            }else{
                counter ++;
            }
            changeIconColor(counter);
        }
    }else if(parseInt(ul.style.left) == -2000){
        ul.style.left = "0px";
    }else{
        if(firstTime == 0){
            firstTime = new Date().getTime();
        }else if(new Date().getTime() - firstTime > 1000){
            firstTime = 0;
            ul.style.left = parseInt(getStyle(ul,"left")) - 1 +'px';
        }
    }
},1);

