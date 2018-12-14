--[[
loadstring 定义values
方便luastate复用
--]]

local values = {
    data = {},
    args = {},
    before = function(tv)
        log = luajava.bindClass("android.util.Log")
        log:e("xx", "from lua before 1")
        if (tv) then
            log:e("xx", "" .. tv:toString())
        else
            log:e("xx", "tv 为 null")
        end
        tv:setText("from lua before");
        log:e("xx", "from lua before 2")
        return "before"
    end,
    exception = function(exception)

    end,
    after = function(returnValue)

    end
}

--https://blog.csdn.net/lang523493505/article/details/51218912
stack[(#stack + 1)] = values
values = nil

---------------------









