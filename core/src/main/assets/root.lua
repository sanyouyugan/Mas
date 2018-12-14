--[[
loadstring 定义values
方便luastate复用
--]]

stack = {}

function callTopFunc(funcName, value2)
    local ok, value = pcall(stack[#stack][funcName], value2)
    if ok then
        return 0, value
    else
        return -1, value
    end
end

function releaseTop()
    stack[#stack].args = nil
    stack[#stack].init = nil
    stack[#stack].before = nil
    stack[#stack].exception = nil
    stack[#stack].after = nil
    stack[#stack].values = nil
    stack[#stack].data = nil
    table.remove(stack)
end

function getTopData()
    return stack[#stack].data
end

function initTop (map)
    local top = stack[#stack]
    for k, v in pairs(map) do
        top.args[k] = v
    end
end









